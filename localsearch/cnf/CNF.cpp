//
// Created by uWX962939 on 5/8/2021.
//

#include "CNF.h"

CNF::CNF(std::vector<Clause> &&clauses, uint32_t literalCount, std::vector<Literal> &&coreVariables,
         std::vector<MetaVariable> &&metaVars, Net &&net, std::vector<uint32_t> &&weights,
         std::vector<uint32_t> &&toCompute) : net(std::move(net)) {
    this->clauses = std::move(clauses);
    this->literalCount = literalCount;
    this->coreVariables = std::move(coreVariables);
    this->metaVars = std::move(metaVars);
    this->net.finalize();
    this->weights = std::move(weights);
    this->toCompute = std::move(toCompute);
}
