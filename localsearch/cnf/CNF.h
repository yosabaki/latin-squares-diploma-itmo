//
// Created by uWX962939 on 5/8/2021.
//

#ifndef METACDCL_CNF_H
#define METACDCL_CNF_H

#include <vector>
#include "Literal.h"
#include "Clause.h"
#include "../net/Net.h"
#include "MetaVariable.h"

struct CNF {
    std::vector<Clause> clauses;
    uint32_t literalCount;
    std::vector<MetaVariable> metaVars;
    std::vector<Literal> coreVariables;
    Net net;
    std::vector<uint32_t> weights;
    std::vector<uint32_t> toCompute;

    CNF(std::vector<Clause> &&clauses,
        uint32_t literalCount,
        std::vector<Literal> &&coreVariables,
        std::vector<MetaVariable> &&metaVars,
        Net &&net,
        std::vector<uint32_t> &&weights,
        std::vector<uint32_t> &&toCompute
    );
};


#endif //METACDCL_CNF_H
