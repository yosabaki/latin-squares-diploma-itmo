//
// Created by uWX962939 on 5/8/2021.
//

#include <algorithm>
#include "Clause.h"

int Clause::propagate(const std::vector<uint8_t> &units) const {
    int satisfiedCount = 0;
    for (auto& literal: this->literals) {
        if (units[literal.number] == literal.value) {
            satisfiedCount++;
        }
    }
    return satisfiedCount;
}

int Clause::propagateSat(const std::vector<uint8_t> &units) {
    satisfied.resize(literals.size());
    int satisfiedCount = 0;
    uint32_t i = 0;
    for (auto& literal: this->literals) {
        if (units[literal.number] == literal.value) {
            satisfied[i++] = true;
            satisfiedCount++;
        } else {
            satisfied[i++] = false;
        }
    }
    return satisfiedCount;
}

bool Clause::propagate(const std::vector<uint8_t> &units, const std::vector<uint32_t> &flipped) const {
    uint32_t i = 0;
    for (auto& literal: this->literals) {
        while (i < flipped.size() && flipped[i] < literal.number) {
            i++;
        }
        bool flippedValue = i < flipped.size() && flipped[i] == literal.number;
        if ((units[literal.number] ^ flippedValue) == literal.value) {
            return true;
        }
    }
    return false;
}

bool Clause::propagate(const std::vector<uint32_t> &flipped) const {
    uint32_t i = 0;
    for (uint32_t index = 0; index < literals.size(); index++) {
        auto& literal = literals[index];
        while (i < flipped.size() && flipped[i] < literal.number) {
            i++;
        }
        if ((i >= flipped.size() || flipped[i] != literal.number) == satisfied[index]) {
            return true;
        }
    }
    return false;
}

Clause::Clause(std::vector<Literal> &&literals) {
    this->literals = std::move(literals);
    std::sort(this->literals.begin(), this->literals.end());
}

std::istream &operator>>(std::istream &in, Clause &clause) {
    int dimacsValue;
    in >> dimacsValue;
    std::vector<Literal> literals;
    while(dimacsValue != 0) {
        literals.emplace_back(dimacsValue);
        in >> dimacsValue;
    }
    clause.literals = std::move(literals);
    return in;
}

std::ostream &operator<<(std::ostream &out, Clause &clause) {
    for (auto& literal: clause.literals) {
        out << literal << ' ';
    }
    out << '0';
    return out;
}
