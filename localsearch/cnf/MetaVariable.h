//
// Created by uWX962939 on 5/17/2021.
//

#ifndef METACDCL_METAVARIABLE_H
#define METACDCL_METAVARIABLE_H


#include <cstdint>
#include <vector>
#include "Literal.h"

struct MetaVariable {
    uint8_t matrixIndex;
    uint8_t rowIndex;
    uint8_t columnIndex;
    std::vector<std::vector<Literal>> variables;
    std::vector<uint8_t> values;

    MetaVariable(uint8_t matrixIndex, uint8_t rowIndex, uint8_t columnIndex,
                 std::vector<std::vector<Literal>> &&variables, std::vector<uint8_t> &&values) :
            matrixIndex(matrixIndex), rowIndex(rowIndex), columnIndex(columnIndex), variables(std::move(variables)),
            values(std::move(values)) {}

    MetaVariable(uint8_t matrixIndex, uint8_t rowIndex, uint8_t columnIndex) :
            matrixIndex(matrixIndex), rowIndex(rowIndex), columnIndex(columnIndex), variables(), values() {}

};


#endif //METACDCL_METAVARIABLE_H
