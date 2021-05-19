//
// Created by uWX962939 on 5/8/2021.
//
#include <vector>
#include "../cnf/CNF.h"
#include "../utils/utils.h"
#include "../cnf/MetaVariable.h"
#include <set>
#include <chrono>
#include <algorithm>
#include <atomic>

#ifndef METACDCL_HILLCLIMBINGSOLVER_H
#define METACDCL_HILLCLIMBINGSOLVER_H

struct HillClimbingSolver {
    std::vector<Clause> clauses;
    std::vector<uint8_t> propagated;
    std::vector<uint8_t> satisfied;
    std::vector<MetaVariable> metaVariables;
    std::vector<std::vector<std::vector<uint32_t>>> structuredMetaVariables;
    std::vector<std::vector<std::vector<uint32_t>>> metaValues;
    std::vector<uint32_t> literal_to_meta_variable;
    std::vector<uint32_t> literal_to_unsat;
    std::vector<uint32_t> weights;
    Net net;
    std::vector<uint32_t> clauses_count_satisfied;
    std::vector<std::vector<uint32_t>> literal_to_clause_with_positive_value;
    std::vector<std::vector<uint32_t>> literal_to_clause_with_negative_value;
    uint32_t sum_weights = 0;
    uint32_t satisfiedSum;
    uint32_t satisfiedCount;
    uint32_t coreSize;

    explicit HillClimbingSolver(CNF &cnf, const std::vector<uint8_t> &first);

    std::vector<uint8_t>
    solve(const uint32_t &thread_count,
          const uint32_t &bagSizeStart,
          const uint32_t &bagSizeEnd,
          const bool log,
          const std::string &outputfile = "");

private:

    bool check_flipped(std::vector<uint32_t> &flippedLiterals) const;

    void apply_flipped(std::vector<uint32_t> &flippedLiterals);

    void reduce();

    bool satisfyPredicateForMetavars(const std::vector<uint32_t> &indices) const;

    bool satisfyPredicateForDomain(
            const std::vector<uint32_t> &metaVariablesIndices,
            const std::vector<uint32_t> &valuesIndices,
            const std::vector<std::vector<std::vector<uint8_t>>> &rowsDistinct,
            const std::vector<std::vector<std::vector<uint8_t>>> &columnsDistinct,
            const std::vector<std::vector<uint8_t>> &pairsIndex
    ) const;

    std::vector<uint32_t>
    random_combination_including(uint32_t n, uint32_t k, const std::vector<uint32_t> &including) const;

    void fixPredicateForMetavars(std::vector<uint32_t> &indices) const;
};


#endif //METACDCL_HILLCLIMBINGSOLVER_H
