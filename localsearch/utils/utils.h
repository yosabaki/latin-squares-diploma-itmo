//
// Created by uWX962939 on 5/8/2021.
//

#ifndef METACDCL_UTILS_H
#define METACDCL_UTILS_H

#include <vector>

uint64_t get_number_of_combinations(uint32_t n, uint32_t k);

bool nextArray(std::vector<uint32_t> &array, std::vector<std::vector<uint32_t>> &indices);

bool contains(const std::vector<uint32_t> &combination, const std::vector<uint32_t> &including);

bool next_combination_including(std::vector<uint32_t> &combination, uint32_t n, const std::vector<uint32_t> &including);

bool next_combination(std::vector<uint32_t> &combination, uint32_t n);

std::vector<uint32_t> random_combination_including(uint32_t n, uint32_t k, const std::vector<uint32_t> &including);

uint32_t log(uint32_t k);

#endif //METACDCL_UTILS_H
