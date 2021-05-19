#include <iostream>
#include <sstream>
#include <fstream>
#include <map>
#include <vector>
#include <cstdlib>
#include <ctime>
#include <cstring>
#include "cnf/CNF.h"
#include "solver/HillClimbingSolver.h"

using namespace std;

void readFirst(const string &filename, std::vector<uint8_t> &first) {
    std::ifstream fin(filename);
    std::string temp;
    fin >> temp;
    Literal k;
    first = std::vector<uint8_t>();
    first.push_back(2);
    while (fin >> k) {
        first.push_back(k.value);
    }
}

CNF readDimacs(const string &filename) {
    std::string temp;
    std::ifstream fin(filename);
    std::string line = "c";
    std::vector<Literal> coreVariables;
    std::vector<MetaVariable> metaVariables;
    std::vector<uint32_t> weights;
    std::vector<uint32_t> toCompute;

    enum {
        CORE_VARIABLES,
        NET,
        META_VARIABLES,
        WEIGHTS,
        TO_COMPUTE,
        NOTHING
    } parameter = NOTHING;

    Net net;
    while (line[0] == 'c') {
        std::getline(fin, line);
        if (line == "c core variables:") {
            parameter = CORE_VARIABLES;
            continue;
        }
        if (line == "c computationalNet:") {
            parameter = NET;
            continue;
        }
        if (line == "c metaVariables:") {
            parameter = META_VARIABLES;
            continue;
        }
        if (line == "c incrementalVariables:") {
            parameter = NOTHING;
            continue;
        }
        if (line == "c weights:") {
            parameter = WEIGHTS;
            continue;
        }
        if (line == "c toCompute:") {
            parameter = TO_COMPUTE;
            continue;
        }
        switch (parameter) {
            case CORE_VARIABLES: {
                std::istringstream s(line);
                s >> temp;
                int literal;
                while (s >> literal) {
                    coreVariables.emplace_back(literal);
                }
                parameter = NOTHING;
                break;
            }
            case NET:
                net.parse_node(line);
                break;
            case META_VARIABLES: {
                if (line.find("c newVar") == 0) {
                    std::istringstream s(line);
                    s >> temp >> temp;
                    uint32_t m, r, c;
                    s >> m >> r >> c;
                    metaVariables.emplace_back(MetaVariable(m, r, c));
                    continue;
                }
                std::istringstream s(line);
                metaVariables.back().variables.emplace_back();
                s >> temp;
                int value;
                s >> value;
                Literal literal;
                metaVariables.back().values.push_back(value);
                while (s >> literal) {
                    metaVariables.back().variables.back().push_back(literal);
                }
                break;
            }
            case WEIGHTS: {
                std::istringstream s(line);
                int weight;
                s >> temp;
                while (s >> weight) {
                    weights.push_back(weight);
                }
                parameter = NOTHING;
                break;
            }
            case TO_COMPUTE: {
                std::istringstream s(line);
                int index;
                s >> temp;
                while (s >> index) {
                    toCompute.push_back(index);
                }
                parameter = NOTHING;
                break;
            }
            default:
                break;
        }
    }
    istringstream iss(line);
    iss.seekg(5);
    int clausesNumber, literalCount;
    iss >> literalCount >> clausesNumber;
    vector<Clause> clauses(clausesNumber);
    for (auto &clause: clauses) {
        fin >> clause;
    }
    return CNF(
            std::move(clauses),
            literalCount,
            std::move(coreVariables),
            std::move(metaVariables),
            std::move(net),
            std::move(weights),
            std::move(toCompute)
    );
}

int main(int argc, char **args) {
    std::cout << argc << std::endl;
    std::cout.flush();
    std::ios::sync_with_stdio(false);
    srand(time(0));
    std::cout << args[1] << endl;
    CNF cnf = readDimacs(args[1]);
    std::string outputfile;
    std::vector<uint8_t> first;
    uint32_t thread_count = 1;
    uint32_t bagSizeStart = 1;
    uint32_t bagSizeEnd = 1;
    if (argc > 2) {
        outputfile = args[2];
    }
    if (argc > 3) {
        thread_count = atoi(args[3]);
    }
    bool log = false;
    if (argc > 6) {
        if (strcmp("-l", args[6]) == 0) {
            log = true;
        } else {
            std::string inputfile = args[6];
            readFirst(inputfile, first);
        }
    }
    if (argc > 4) {
        if (strcmp("-l", args[4]) == 0) {
            log = true;
        } else {
            bagSizeStart = atoi(args[4]);
        }
    }
    if (argc > 5) {
        if (strcmp("-l", args[5]) == 0) {
            log = true;
        } else {
            bagSizeEnd = atoi(args[5]);
        }
    }
    if (!log) {
        for (uint32_t arg = 1; arg < argc; arg++) {
            if (strcmp("-l", args[arg]) == 0) {
                log = true;
            }
        }
    }
    std::cout << "c core variables:" << std::endl;
    std::cout.flush();
    for (auto &variable: cnf.coreVariables) {
        std::cout << variable << ' ';
    }
    std::cout << std::endl;
    std::cout << "p CNF " << cnf.clauses.size() << ' ' << cnf.literalCount << std::endl;
    // for (auto& clause: CNF.clauses) {
    // for (auto& literal: clause.literals) {
    // std::cout << literal << ' ';
    // }
    // std:: cout << "0\n";
    // }
    auto solver = HillClimbingSolver(cnf, first);
    auto solved = solver.solve(thread_count, bagSizeStart, bagSizeEnd, log, outputfile);
    if (argc > 2) {
        std::ofstream fout(outputfile);
        fout << "SAT\n";
        for (uint32_t i = 1; i < solved.size(); i++) {
            if (solved[i] == 0) {
                fout << '-';
            } else if (solved[i] == 2) {
                fout << '?';
            }
            fout << i << ' ';
        }
    }
    return 0;
}