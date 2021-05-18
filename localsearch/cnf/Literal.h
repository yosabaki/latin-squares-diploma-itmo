//
// Created by uWX962939 on 5/8/2021.
//

#ifndef METACDCL_LITERAL_H
#define METACDCL_LITERAL_H

#include <cmath>
#include <iostream>

struct Literal {
    uint32_t number;
    bool value;

    Literal() = default;

    Literal(bool value, uint32_t number);

    explicit Literal(int dimacsValue);

    bool operator<(const Literal &other) const;

    bool operator<=(const Literal &other) const;

    bool operator>=(const Literal &other) const;

    bool operator>(const Literal &other) const;

    bool operator==(const Literal &other) const;
};

std::ostream& operator<<(std::ostream& out, Literal &literal);
std::istream& operator>>(std::istream& in, Literal &literal);

#endif //METACDCL_LITERAL_H
