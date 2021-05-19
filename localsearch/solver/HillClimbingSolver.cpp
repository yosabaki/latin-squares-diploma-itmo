//
// Created by uWX962939 on 5/8/2021.
//

#include "HillClimbingSolver.h"

#include <random>
#include "../utils/thread_pool.hpp"
#include <csignal>
#include <sstream>
#include <fstream>

HillClimbingSolver::HillClimbingSolver(CNF &cnf, const std::vector<uint8_t> &first) : net(std::move(cnf.net)) {
    satisfiedSum = 0;
    satisfiedCount = 0;
    this->clauses = cnf.clauses;
    std::vector<uint32_t> core(cnf.coreVariables.size());
    std::set<uint32_t> toCompute;
    for (uint32_t i : cnf.toCompute) {
        toCompute.insert(i);
    }
    propagated = std::vector<uint8_t>(cnf.literalCount + 1, 2);
    literal_to_unsat = std::vector<uint32_t>(cnf.literalCount + 1, 0);
    literal_to_meta_variable = std::vector<uint32_t>(cnf.literalCount + 1, -1);
    literal_to_clause_with_positive_value = std::vector<std::vector<uint32_t>>(cnf.literalCount + 1);
    literal_to_clause_with_negative_value = std::vector<std::vector<uint32_t>>(cnf.literalCount + 1);
    this->coreSize = cnf.coreVariables.size();
    this->weights = std::move(cnf.weights);
    this->metaVariables = std::move(cnf.metaVars);
    satisfied = std::vector<uint8_t>(clauses.size(), 0);
    clauses_count_satisfied = std::vector<uint32_t>(clauses.size(), 0);
    for (uint32_t i = 0; i < clauses.size(); i++) {
        sum_weights += weights[i];
        if (toCompute.find(i) == toCompute.end()) {
            continue;
        }
        for (auto &literal: clauses[i].literals) {
            if (literal.value) {
                literal_to_clause_with_positive_value[literal.number].push_back(i);
            } else {
                literal_to_clause_with_negative_value[literal.number].push_back(i);
            }
        }
    }
    for (uint32_t i = 0; i < metaVariables.size(); i++) {
        auto &metaVariable = metaVariables[i];
        if (metaValues.size() <= metaVariable.matrixIndex) {
            structuredMetaVariables.resize(metaVariable.matrixIndex + 1);
            metaValues.resize(metaVariable.matrixIndex + 1);
        }
        if (metaValues[metaVariable.matrixIndex].size() <= metaVariable.rowIndex) {
            structuredMetaVariables[metaVariable.matrixIndex].resize(metaVariable.rowIndex + 1);
            metaValues[metaVariable.matrixIndex].resize(metaVariable.rowIndex + 1);
        }

        if (metaValues[metaVariable.matrixIndex][metaVariable.rowIndex].size() <= metaVariable.columnIndex) {
            structuredMetaVariables[metaVariable.matrixIndex][metaVariable.rowIndex].resize(
                    metaVariable.columnIndex + 1, -1);
            metaValues[metaVariable.matrixIndex][metaVariable.rowIndex].resize(metaVariable.columnIndex + 1);
        }
        for (Literal &variable : metaVariable.variables[0]) {
            literal_to_meta_variable[variable.number] = i;
        }
    }
    reduce();
    for (uint32_t k = 0; k < metaValues.size(); k++) {
        metaValues[k][0].resize(metaValues[k][1].size());
        for (uint32_t i = 0; i < metaValues[k].size(); i++) {
            metaValues[k][i][0] = i + 1;
        }
        if (k == 0) {
            for (uint32_t i = 0; i < metaValues[k][0].size(); i++) {
                metaValues[k][0][i] = i + 1;
            }
        }
    }
    for (uint32_t i = 0; i < metaVariables.size(); i++) {
        auto &metaVariable = metaVariables[i];
        int meta = rand() % metaVariable.variables.size();
        metaValues[metaVariable.matrixIndex][metaVariable.rowIndex][metaVariable.columnIndex] = metaVariable.values[meta];
        structuredMetaVariables[metaVariable.matrixIndex][metaVariable.rowIndex][metaVariable.columnIndex] = i;
        for (Literal &variable : metaVariable.variables[meta]) {
            core[variable.number - 1] = variable.number;
            propagated[variable.number] = variable.value;
        }
    }
    if (!first.empty()) {
        for (uint32_t i = 1; i <= coreSize; i++) {
            propagated[i] = first[i];
            if (first[i] && literal_to_meta_variable[i] != -1) {
                auto &metaVar = metaVariables[literal_to_meta_variable[i]];
                bool flag = false;
                for (uint32_t k = 0; k < metaVar.variables.size(); k++) {
                    for (uint32_t j = 0; j < metaVar.variables[k].size(); j++) {
                        if (metaVar.variables[k][j].number == i && metaVar.variables[k][j].value) {
                            metaValues[metaVar.matrixIndex][metaVar.rowIndex][metaVar.columnIndex] = metaVar.values[k];
                            flag = true;
                            break;
                        }
                    }
                    if (flag) break;
                }
            }
        }
    }
    net.compute(propagated);
    for (uint32_t i = 0; i < clauses.size(); i++) {
        int count = clauses[i].propagateSat(propagated);
        clauses_count_satisfied[i] = count;
        if (count > 0 || toCompute.find(i) == toCompute.end()) {
            satisfied[i] = true;
            satisfiedSum += weights[i];
            satisfiedCount++;
        } else {
            for (auto &literal: clauses[i].literals) {
                literal_to_unsat[literal.number]++;
            }
        }
    }
}

void HillClimbingSolver::reduce() {
    std::vector<uint32_t> unitPropagationCache;
    std::map<uint32_t, uint32_t> toDelete;
    bool changed = true;
    while (changed) {
        changed = false;
        for (auto &clause : clauses) {
            if (clause.literals.size() == 1) {
                auto &literal = clause.literals[0];
                if (std::find(unitPropagationCache.begin(), unitPropagationCache.end(), literal.number) ==
                    unitPropagationCache.end()) {
                    propagated[literal.number] = literal.value;
                    uint32_t metaVarIndex = literal_to_meta_variable[literal.number];
                    unitPropagationCache.push_back(literal.number);
                    if (metaVarIndex != -1) {
                        std::vector<std::vector<Literal>> newMetaVar;
                        std::vector<uint8_t> newValues;
                        for (uint32_t index = 0; index < metaVariables[metaVarIndex].variables.size(); index++) {
                            auto &metaVar = metaVariables[metaVarIndex].variables[index];
                            auto &value = metaVariables[metaVarIndex].values[index];
                            for (auto &var: metaVar) {
                                if (var.number == literal.number && var.value == literal.value) {
                                    newMetaVar.push_back(metaVar);
                                    newValues.push_back(value);
                                    break;
                                }
                            }
                        }
                        std::swap(metaVariables[metaVarIndex].variables, newMetaVar);
                        std::swap(metaVariables[metaVarIndex].values, newValues);
                    }
                    std::vector<uint32_t> &to_sat = (literal.value
                                                     ? literal_to_clause_with_positive_value[literal.number]
                                                     : literal_to_clause_with_negative_value[literal.number]);
                    std::vector<uint32_t> &to_sub = (literal.value
                                                     ? literal_to_clause_with_negative_value[literal.number]
                                                     : literal_to_clause_with_positive_value[literal.number]);
                    for (uint32_t i: to_sat) {
                        toDelete[i] = literal.number;
                    }
                    for (uint32_t i: to_sub) {
                        changed = true;
                        auto &clauseLiterals = clauses[i].literals;
                        for (uint32_t k = 0; k < clauseLiterals.size(); k++) {
                            if (clauseLiterals[k].number == literal.number) {
                                clauseLiterals.erase(clauseLiterals.begin() + k);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    for (auto &entity: toDelete) {
        uint32_t k;
        for (uint32_t i = 0; i < clauses[entity.first].literals.size(); i++) {
            auto &cLiteral = clauses[entity.first].literals[i];
            if (cLiteral.number == entity.second) {
                k = i;
            }
            if (cLiteral.value) {
                auto &lcpv = literal_to_clause_with_positive_value[cLiteral.number];
                lcpv.erase(std::find(lcpv.begin(), lcpv.end(), entity.first));
            } else {
                auto &lcnv = literal_to_clause_with_negative_value[cLiteral.number];
                lcnv.erase(std::find(lcnv.begin(), lcnv.end(), entity.first));
            }
        }
        std::swap(clauses[entity.first].literals[k], clauses[entity.first].literals[0]);
        clauses[entity.first].literals.erase(clauses[entity.first].literals.begin() + 1,
                                             clauses[entity.first].literals.end());
    }
    for (uint32_t index: unitPropagationCache) {
        literal_to_clause_with_negative_value[index].clear();
        literal_to_clause_with_positive_value[index].clear();
    }
}

bool HillClimbingSolver::check_flipped(std::vector<uint32_t> &flippedLiterals) const {
    std::vector<uint8_t> newSatisfiedCountClauses(clauses_count_satisfied.size(), 0);
    net.compute(propagated, flippedLiterals);
    uint32_t newSatisfiedCount = satisfiedCount;
    uint32_t newSatisfiedSum = satisfiedSum;
//    std::sort(flippedLiterals.begin(), flippedLiterals.end());
    for (uint32_t i: flippedLiterals) {
//        const std::vector<uint32_t> &to_add = (propagated[i] ^ 1) ? literal_to_clause_with_positive_value[i]
//                                                                  : literal_to_clause_with_negative_value[i];
//        const std::vector<uint32_t> &to_sub = (propagated[i] ^ 1) ? literal_to_clause_with_negative_value[i]
//                                                                  : literal_to_clause_with_positive_value[i];
        for (uint32_t index: literal_to_clause_with_positive_value[i]) {
            if (!newSatisfiedCountClauses[index]) {
                newSatisfiedCountClauses[index] = 1;
                bool newSatisfied = clauses[index].propagate(propagated, flippedLiterals);
                if (newSatisfied && !satisfied[index]) {
                    newSatisfiedSum += weights[index];
                    newSatisfiedCount++;
                } else if (!newSatisfied && satisfied[index]) {
                    newSatisfiedSum -= weights[index];
                    newSatisfiedCount--;
                }
            }
        }
        for (uint32_t index: literal_to_clause_with_negative_value[i]) {
            if (!newSatisfiedCountClauses[index]) {
                newSatisfiedCountClauses[index] = 1;
                bool newSatisfied = clauses[index].propagate(propagated, flippedLiterals);
                if (newSatisfied && !satisfied[index]) {
                    newSatisfiedSum += weights[index];
                    newSatisfiedCount++;
                } else if (!newSatisfied && satisfied[index]) {
                    newSatisfiedSum -= weights[index];
                    newSatisfiedCount--;
                }
            }
        }
    }
    return newSatisfiedSum > satisfiedSum;
}

void HillClimbingSolver::apply_flipped(std::vector<uint32_t> &flippedLiterals) {
    std::vector<uint32_t> newSatisfied;
    std::vector<uint32_t> newSatisfiedCountClauses(clauses_count_satisfied.size(), 0);
    net.compute(propagated, flippedLiterals);
    uint32_t newSatisfiedCount = satisfiedCount;
    uint32_t newSatisfiedSum = satisfiedSum;

    for (auto i : flippedLiterals) {
        propagated[i] ^= 1;
    }

    for (uint32_t i: flippedLiterals) {
        const std::vector<uint32_t> &to_add = propagated[i] ? literal_to_clause_with_positive_value[i]
                                                            : literal_to_clause_with_negative_value[i];
        const std::vector<uint32_t> &to_sub = propagated[i] ? literal_to_clause_with_negative_value[i]
                                                            : literal_to_clause_with_positive_value[i];
        for (uint32_t index: to_sub) {
            clauses[index].propagateSat(propagated);
            newSatisfiedCountClauses[index]--;
            if (clauses_count_satisfied[index] + newSatisfiedCountClauses[index] == 0) {
                for (auto &literal: clauses[index].literals) {
                    literal_to_unsat[literal.number]++;
                }
                newSatisfied.push_back(index);
                newSatisfiedSum -= weights[index];
                newSatisfiedCount--;
            }
        }
        for (uint32_t index: to_add) {
            clauses[index].propagateSat(propagated);
            newSatisfiedCountClauses[index]++;
            if (clauses_count_satisfied[index] + newSatisfiedCountClauses[index] == 1) {
                for (auto &literal: clauses[index].literals) {
                    literal_to_unsat[literal.number]--;
                }
                newSatisfied.push_back(index);
                newSatisfiedSum += weights[index];
                newSatisfiedCount++;
            }
        }
    }
    for (uint32_t i = 0; i < newSatisfiedCountClauses.size(); i++) {
        clauses_count_satisfied[i] += newSatisfiedCountClauses[i];
    }
    satisfiedCount = newSatisfiedCount;
    satisfiedSum = newSatisfiedSum;
    for (auto &index: newSatisfied) {
        satisfied[index] ^= 1;
    }
}

std::atomic<bool> stopped = false;

void handler(int s) {
    if (s == SIGINT || s == SIGTERM) {
        if (!stopped) {
            stopped = true;
        } else {
            exit(1);
        }
    }
}

std::vector<uint32_t>
HillClimbingSolver::random_combination_including(uint32_t n, uint32_t k, const std::vector<uint32_t> &including) const {
    std::vector<uint32_t> combination;
    combination.push_back(including[rand() % including.size()]);
    for (uint32_t i = 1; i < k; i++) {
        combination.push_back(-1);
        uint32_t val = combination[0];
        while (std::find(combination.begin(), combination.end(), val) != combination.end()) {
            val = rand() % n;
        }
        combination.back() = val;
    }
    std::sort(combination.begin(), combination.end());
    for (uint32_t tries = 0; tries < 1; tries++) {
        if (satisfyPredicateForMetavars(combination)) {
            break;
        }
        next_combination_including(combination, n, including);
    }
    return combination;
}

std::vector<uint8_t> HillClimbingSolver::solve(
        const uint32_t &thread_count,
        const uint32_t &bagSizeStart,
        const uint32_t &bagSizeEnd,
        const bool log,
        const std::string &outputfile) {
    bool changed = true;
    signal(SIGINT, handler);
    signal(SIGTERM, handler);
    int q = 0;
    uint32_t size = metaVariables.size();
    uint32_t limit = 0;
    uint32_t bagSize = bagSizeStart;
    thread_pool pool(thread_count);
    std::mutex solutionMutex;
    while (q >= 0 && changed && clauses.size() - satisfiedCount > limit) {
        std::vector<std::vector<std::vector<uint8_t>>> rowsDistinct(
                metaValues.size(),
                std::vector<std::vector<uint8_t>>(metaValues[0].size(),
                                                  std::vector<uint8_t>(metaValues[0].size(), 0))
        );
        std::vector<std::vector<uint8_t>> pairsIndex(
                metaValues.size() * (metaValues.size() - 1) / 2,
                std::vector<uint8_t>(metaValues[0].size() * metaValues[0].size(), 0)
        );
        std::vector<std::vector<std::vector<uint8_t>>> columnsDistinct(
                metaValues.size(),
                std::vector<std::vector<uint8_t>>(metaValues[0].size(),
                                                  std::vector<uint8_t>(metaValues[0].size(), 0))
        );

        for (uint8_t k = 0; k < metaValues.size(); k++) {
            for (uint8_t i = 0; i < metaValues[k].size(); i++) {
                for (uint8_t j = 0; j < metaValues[k].size(); j++) {
                    columnsDistinct[k][j][metaValues[k][i][j] - 1]++;
                    rowsDistinct[k][i][metaValues[k][i][j] - 1]++;
                }
            }
        }
        for (uint8_t k1 = 0; k1 < metaValues.size(); k1++) {
            for (uint8_t k2 = k1 + 1; k2 < metaValues.size(); k2++) {
                for (uint8_t i = 0; i < metaValues[k1].size(); i++) {
                    for (uint8_t j = 0; j < metaValues[k1].size(); j++) {
                        uint8_t val1 = metaValues[k1][i][j] - 1;
                        uint8_t val2 = metaValues[k2][i][j] - 1;
                        pairsIndex[k1 + k2 - 1][val1 * metaValues[k1].size() + val2]++;
                    }
                }
            }
        }
        std::vector<uint32_t> solution;
        std::vector<std::tuple<uint8_t, uint8_t, uint8_t, uint8_t>> newMetaValues;
        std::cout << q + 1 << std::endl;
        std::cout << satisfiedSum << '\\' << sum_weights << '\\' << satisfiedCount << '\\' << clauses.size()
                  << std::endl;
        changed = false;
        std::cout << bagSize << std::endl;
        std::cout.flush();
        std::set<uint32_t> vars;
        for (uint32_t i = 0; i < literal_to_unsat.size(); i++) {
            if (literal_to_unsat[i] > 0) {
                if (literal_to_meta_variable[i] == -1) {
                    for (int var = 0; var < metaVariables.size(); var++) {
                        vars.insert(var);
                    }
                } else {
                    vars.insert(literal_to_meta_variable[i]);
                }
            }
        }
        std::vector<uint32_t> vars_vec;
        std::vector<std::vector<uint32_t>> allPermutationIndices;
        for (auto &var: vars) {
            vars_vec.push_back(var);
            std::cout << var << ' ';
        }
        std::cout << std::endl;

        std::vector<uint32_t> tmpPermutationIndices(bagSize);
        for (int i = 0; i < bagSize - 1; i++) {
            tmpPermutationIndices[i] = i;
        }
        tmpPermutationIndices[bagSize - 1] = std::max(vars_vec[0], bagSize - 1);

        if (get_number_of_combinations(size, tmpPermutationIndices.size()) > 10'000'000) {
            for (int i = 0; i < 10'000'000; i++) {
                auto tmpCombination = random_combination_including(size, tmpPermutationIndices.size(), vars_vec);
                if (!satisfyPredicateForMetavars(tmpCombination)) {
                    continue;
//                    next_combination_including(tmpCombination, size, vars_vec);
                } else {
                    allPermutationIndices.emplace_back(tmpCombination);
                }
            }
            std::cout << "PRE N:" << allPermutationIndices.size() << std::endl;
            std::sort(allPermutationIndices.begin(), allPermutationIndices.end());
            std::vector<std::vector<uint32_t>> tmp;
            for (uint32_t i = 0; i < allPermutationIndices.size() - 1; i++) {
                if (allPermutationIndices[i] != allPermutationIndices[i + 1]) {
                    tmp.emplace_back(std::move(allPermutationIndices[i]));
                    if (i == allPermutationIndices.size() - 2) {
                        tmp.emplace_back(std::move(allPermutationIndices[i + 1]));
                    }
                }
            }
            std::swap(allPermutationIndices, tmp);
        } else {
            do {
                if (satisfyPredicateForMetavars(tmpPermutationIndices)) {
                    allPermutationIndices.emplace_back(tmpPermutationIndices);
                }
            } while (next_combination_including(tmpPermutationIndices, size, vars_vec));
        }
        std::shuffle(allPermutationIndices.begin(), allPermutationIndices.end(), std::mt19937(std::random_device()()));
        uint32_t n = allPermutationIndices.size();
        if (bagSize >= 2) {
            std::cout << "N:" << n << std::endl;
            std::cout.flush();
        }
        uint32_t batch_size = allPermutationIndices.size() / thread_count;
        uint32_t remains = allPermutationIndices.size() % thread_count;
        std::atomic<uint32_t> counter = 0;
        std::atomic<bool> isSolutionFound = false;
        auto begin = std::chrono::steady_clock::now();
        uint32_t prev = 0;
        for (uint32_t thread = 0; thread < thread_count; thread++) {
            uint32_t start = prev;
            uint32_t end = prev + batch_size;
            if (remains) {
                end++;
                remains--;
            }
            prev = end;
            pool.push_task([&, end, start]() {
                for (uint32_t index = start; index < end; index++) {
                    auto mainPermutationIndices = allPermutationIndices[index];
                    std::vector<uint32_t> flipped;
                    std::vector<std::vector<std::vector<Literal>>> tmp(bagSize);
                    std::vector<std::vector<uint32_t>> indices(bagSize);
                    for (int bag = 0; bag < bagSize; bag++) {
                        tmp[bag] = metaVariables[mainPermutationIndices[bag]].variables;
                        indices[bag] = std::vector<uint32_t>(tmp[bag].size());
                        for (uint32_t i = 0; i < tmp[bag].size(); i++) {
                            indices[bag][i] = i;
                        }
                    }
                    std::vector<uint32_t> permIndices(indices.size(), 0);
                    do {
                        if (!satisfyPredicateForDomain(
                                mainPermutationIndices,
                                permIndices,
                                rowsDistinct,
                                columnsDistinct,
                                pairsIndex)) {
                            continue;
                        }
                        flipped = std::vector<uint32_t>();
                        for (uint32_t i = 0; i < indices.size(); i++) {
                            for (auto &literal: tmp[i][permIndices[i]]) {
                                if (propagated[literal.number] != literal.value) {
                                    flipped.push_back(literal.number);
                                }
                            }
                        }
                        bool returned = check_flipped(flipped);
                        if (returned) {
                            counter += index + 1 - start;
                            if (isSolutionFound) {
                                return;
                            }
                            isSolutionFound = true;
                            const std::scoped_lock lock(solutionMutex);
                            solution = flipped;
                            for (uint32_t i = 0; i < mainPermutationIndices.size(); i++) {
                                auto &mv = metaVariables[mainPermutationIndices[i]];
                                newMetaValues.emplace_back(mv.matrixIndex, mv.rowIndex, mv.columnIndex,
                                                           mv.values[permIndices[i]]);
                            }
                            return;
                        }
                        if (stopped) break;
                    } while (nextArray(permIndices, indices));
                    if (isSolutionFound || stopped) {
                        counter += index + 1 - start;
                        return;
                    }
                }
                counter += end - start;
            });
        }
        pool.wait_for_tasks_conditional();
        pool.reset_queue();
        q++;
        if (isSolutionFound) {
            apply_flipped(solution);
            for (auto &mv: newMetaValues) {
                metaValues[std::get<0>(mv)][std::get<1>(mv)][std::get<2>(mv)] = std::get<3>(mv);
            }
            changed = true;
        }
        if (bagSize >= 2) {
            auto end = std::chrono::steady_clock::now();
            auto difference = std::chrono::duration_cast<std::chrono::milliseconds>(end - begin).count();
            std::cout << '\r' << counter << '\\' << n << '\\' << float(counter) / difference * 1000.0 << "pps"
                      << std::endl;
        }
        if (!changed && bagSize < bagSizeEnd) {
            bagSize++;
            changed = true;
        }
        if (stopped) {
            changed = false;
        }
        if (log) {
            std::string logFile;
            {
                std::ostringstream s;
                s << outputfile << "_" << q;
                logFile = s.str();
            }
            std::cout << logFile << std::endl;
            std::ofstream fout(logFile);
            fout << "SAT\n";
            for (uint32_t i = 1; i < propagated.size(); i++) {
                if (propagated[i] == 0) {
                    fout << '-';
                } else if (propagated[i] == 2) {
                    fout << '?';
                }
                fout << i << ' ';
            }
            {
                std::ostringstream s;
                s << outputfile << "_c" << q;
                std::ofstream cfout(s.str());
                cfout << "c q: " << q << std::endl;
                cfout << "c n: " << n << std::endl;
                cfout << "c bagSize: " << bagSize << std::endl;
            }
        }
    }

    int counter = 0;
    for (auto &clause: clauses) {
        if (clause.propagate(propagated)) {
            counter++;
        } else {
            std::cout << clause << std::endl;
        }
    }
    std::cout << counter << '\\' << clauses.size();
    return propagated;
}

bool HillClimbingSolver::satisfyPredicateForMetavars(const std::vector<uint32_t> &indices) const {
    for (uint32_t i: indices) {
        if (metaVariables[i].variables.size() <= 1) {
            return false;
        }
    }
    if (indices.size() == 1) {
        return true;
    }
    std::vector<std::tuple<uint8_t, uint8_t, uint8_t>> found;
    std::vector<uint32_t> satisfiedIndices(indices.size(), false);
    for (uint32_t j = 0; j < indices.size(); j++) {
        uint32_t i = indices[j];
        for (uint32_t k = 0; k < found.size(); k++) {
            auto &tuple = found[k];
            if (metaVariables[i].matrixIndex == std::get<0>(tuple)) {
                if (metaVariables[i].rowIndex == std::get<1>(tuple) ||
                    metaVariables[i].columnIndex == std::get<2>(tuple)) {
                    satisfiedIndices[j] = true;
                    satisfiedIndices[k] = true;
                    break;
                }
            } else {
                if (metaVariables[i].rowIndex == std::get<1>(tuple) &&
                    metaVariables[i].columnIndex == std::get<2>(tuple)) {
                    satisfiedIndices[j] = true;
                    satisfiedIndices[k] = true;
                    break;
                }
            }
        }
        found.emplace_back(metaVariables[i].matrixIndex, metaVariables[i].rowIndex, metaVariables[i].columnIndex);
    }
    for (auto val: satisfiedIndices) {
        if (!val) return false;
    }
    return true;
}

void HillClimbingSolver::fixPredicateForMetavars(std::vector<uint32_t> &indices) const {
    for (uint32_t i: indices) {
        if (metaVariables[i].variables.size() <= 1) {
//            return false;
        }
    }
}

bool HillClimbingSolver::satisfyPredicateForDomain(
        const std::vector<uint32_t> &metaVariablesIndices,
        const std::vector<uint32_t> &valuesIndices,
        const std::vector<std::vector<std::vector<uint8_t>>> &rowsDistinct,
        const std::vector<std::vector<std::vector<uint8_t>>> &columnsDistinct,
        const std::vector<std::vector<uint8_t>> &pairsIndex
) const {
    int sum = 0;
    std::vector<std::vector<std::vector<int8_t>>> valuesColumns(metaValues.size());
    std::vector<std::vector<std::vector<int8_t>>> valuesRows(metaValues.size());
    std::vector<std::vector<int8_t>> pairsIndexDelta(pairsIndex.size(),
                                                     std::vector<int8_t>(metaValues[0].size() * metaValues[0].size(),
                                                                         0));
    std::vector<std::vector<uint8_t>> changedColumns(metaValues.size()), changedRows(metaValues.size());
    for (uint32_t i = 0; i < metaVariablesIndices.size(); i++) {
        auto &metaVar = metaVariables[metaVariablesIndices[i]];
        const auto &rowPos = std::find(
                changedRows[metaVar.matrixIndex].begin(),
                changedRows[metaVar.matrixIndex].end(),
                metaVar.rowIndex);
        uint32_t index;
        if (rowPos == changedRows[metaVar.matrixIndex].end()) {
            index = changedRows[metaVar.matrixIndex].size();
            changedRows[metaVar.matrixIndex].push_back(metaVar.rowIndex);
            valuesRows[metaVar.matrixIndex].emplace_back(metaValues[0].size(), 0);
        } else {
            index = rowPos - changedRows[metaVar.matrixIndex].begin();
        }
        valuesRows[metaVar.matrixIndex][index][metaVar.values[valuesIndices[i]] - 1]++;
        valuesRows[metaVar.matrixIndex][index][metaValues[metaVar.matrixIndex][metaVar.rowIndex][metaVar.columnIndex] -
                                               1]--;

        const auto &columnPos = std::find(
                changedColumns[metaVar.matrixIndex].begin(),
                changedColumns[metaVar.matrixIndex].end(),
                metaVar.columnIndex);
        if (columnPos == changedColumns[metaVar.matrixIndex].end()) {
            index = changedColumns[metaVar.matrixIndex].size();
            changedColumns[metaVar.matrixIndex].push_back(metaVar.columnIndex);
            valuesColumns[metaVar.matrixIndex].emplace_back(metaValues[0].size(), 0);
        } else {
            index = columnPos - changedColumns[metaVar.matrixIndex].begin();
        }
        valuesColumns[metaVar.matrixIndex][index][metaVar.values[valuesIndices[i]] - 1]++;
        valuesColumns[metaVar.matrixIndex][index][
                metaValues[metaVar.matrixIndex][metaVar.rowIndex][metaVar.columnIndex] - 1]--;
        for (uint32_t k1 = 0; k1 < metaValues.size(); k1++) {
            for (uint32_t k2 = k1 + 1; k2 < metaValues.size(); k2++) {
                uint32_t index = k1 + k2 - 1;
                if (k1 == metaVar.matrixIndex) {
                    uint8_t oldVal1 = metaValues[k1][metaVar.rowIndex][metaVar.columnIndex] - 1;
                    uint8_t newVal1 = metaVar.values[valuesIndices[i]] - 1;
                    uint8_t val2 = metaValues[k2][metaVar.rowIndex][metaVar.columnIndex] - 1;
                    int8_t val = --pairsIndexDelta[index][oldVal1 * metaValues[0].size() + val2];
                    if (val + pairsIndex[index][oldVal1 * metaValues[0].size() + val2] == 0) {
                        sum++;
                    }
                    val = ++pairsIndexDelta[index][newVal1 * metaValues[0].size() + val2];
                    if (val + pairsIndex[index][newVal1 * metaValues[0].size() + val2] == 1) {
                        sum--;
                    }
                } else if (k2 == metaVar.matrixIndex) {
                    uint8_t oldVal2 = metaValues[k2][metaVar.rowIndex][metaVar.columnIndex] - 1;
                    uint8_t newVal2 = metaVar.values[valuesIndices[i]] - 1;
                    uint8_t val1 = metaValues[k1][metaVar.rowIndex][metaVar.columnIndex] - 1;
                    int8_t val = --pairsIndexDelta[index][val1 * metaValues[0].size() + oldVal2];
                    if (val + pairsIndex[index][val1 * metaValues[0].size() + oldVal2] == 0) {
                        sum++;
                    }
                    val = ++pairsIndexDelta[index][val1 * metaValues[0].size() + newVal2];
                    if (val + pairsIndex[index][val1 * metaValues[0].size() + newVal2] == 1) {
                        sum--;
                    }
                }
            }
        }

    }
    for (uint32_t k = 0; k < metaValues.size(); k++) {
        for (uint32_t i = 0; i < changedRows[k].size(); i++) {
            const auto &row = changedRows[k][i];
            const auto &delta = valuesRows[k][i];
            const auto &prev = rowsDistinct[k][row];
            for (uint32_t j = 0; j < delta.size(); j++) {
                if (delta[j] == 0) {
                    continue;
                }
                sum += ((prev[j] + delta[j]) * (prev[j] + delta[j] - 1) / 2) - (prev[j] * (prev[j] - 1) / 2);
                sum += uint8_t(prev[j] + delta[j] == 0) - uint8_t(prev[j] == 0);
            }
        }

        for (uint32_t i = 0; i < changedColumns[k].size(); i++) {
            const auto &column = changedColumns[k][i];
            const auto &delta = valuesColumns[k][i];
            const auto &prev = columnsDistinct[k][column];
            for (uint32_t j = 0; j < delta.size(); j++) {
                if (delta[j] == 0) {
                    continue;
                }
                sum += ((prev[j] + delta[j]) * (prev[j] + delta[j] - 1) / 2) - (prev[j] * (prev[j] - 1) / 2);
                sum += (prev[j] + delta[j] == 0) - (prev[j] == 0);
            }
        }
    }
    return sum <= 0;
}
