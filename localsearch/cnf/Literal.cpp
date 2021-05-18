//
// Created by uWX962939 on 5/8/2021.
//

#include <iostream>
#include "Literal.h"

std::ostream& operator<<(std::ostream& out, Literal &literal) {
    out << int32_t(literal.number) * (literal.value ? 1 : -1);
    return out;
}

std::istream& operator>>(std::istream& in, Literal &literal) {
    int value;
    in >> value;
    literal.number = abs(value);
    literal.value = value > 0;

    return in;
}

Literal::Literal(int dimacsValue) : value(dimacsValue > 0), number(abs(dimacsValue)) {}

Literal::Literal(bool value, uint32_t number) : value(value), number(number) {}

bool Literal::operator<(const Literal &other) const {
    return number < other.number;
}

bool Literal::operator<=(const Literal &other) const {
    return number <= other.number;
}

bool Literal::operator>=(const Literal &other) const {
    return number >= other.number;
}

bool Literal::operator>(const Literal &other) const {
    return number > other.number;
}

bool Literal::operator==(const Literal &other) const {
    return number == other.number;
}
