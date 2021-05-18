//
// Created by uWX962939 on 5/8/2021.
//

#ifndef METACDCL_CLAUSE_H
#define METACDCL_CLAUSE_H


#include <vector>
#include <iostream>
#include "Literal.h"

struct Clause {
    std::vector<Literal> literals; // sorted literals
    std::vector<uint8_t> satisfied;

    Clause() = default;

    explicit Clause(std::vector<Literal> &&literals);

    int propagate(const std::vector<uint8_t> &units) const;

    int propagateSat(const std::vector<uint8_t> &units);

    bool propagate(const std::vector<uint8_t> &units, const std::vector<uint32_t> &flipped) const;

    bool propagate(const std::vector<uint32_t> &flipped) const;
};

std::istream &operator>>(std::istream &in, Clause &clause);

std::ostream &operator<<(std::ostream &out, Clause &clause);


#endif //METACDCL_CLAUSE_H
