//
// Created by uWX962939 on 5/8/2021.
//

#include <cstdint>
#include <cstdlib>
#include "utils.h"

bool nextArray(std::vector<unsigned int> &array, std::vector<std::vector<unsigned int>> &indices) {
    for (size_t i = 0; i < array.size(); i++) {
        if (++array[i] == indices[i].size()) {
            array[i] = 0;
        } else {
            return true;
        }
    }
    return false;
}

uint64_t get_number_of_combinations(uint32_t n, uint32_t k) {
    uint64_t result = 1;
    for (uint32_t i = 0; i < k; i++) {
        result *= n - i;
        if (UINT64_MAX / (n - i) < result) {
            return UINT64_MAX;
        }
    }
    for (uint32_t i = 2; i <= k; i++) {
        result /= i;
    }
    return result;
}

bool contains(const std::vector<uint32_t> &combination, const std::vector<uint32_t> &including) {
    uint32_t i = 0;
    for (auto &element: combination) {
        while (i < including.size() && including[i] < element) {
            i++;
        }
        if (i < including.size() && element == including[i]) {
            return true;
        }
    }
    return false;
}

bool
next_combination_including(std::vector<uint32_t> &combination, uint32_t n, const std::vector<uint32_t> &including) {
    do {
        if (!next_combination(combination, n)) {
            return false;
        }
    } while (!contains(combination, including));
    return true;
}

std::vector<uint32_t> random_combination_including(uint32_t n, uint32_t k, const std::vector<uint32_t> &including) {
    std::vector<uint32_t> combination(k);
    while (true) {
        std::uint32_t min = 0, max = n - k;
        for (uint32_t i = 0; i < k; i++) {
            combination[i] = (rand() % (max - min)) + min;
            min = combination[i] + 1;
            max++;
        }
        if (!next_combination_including(combination, n, including)) {
            continue;
        } else {
            return combination;
        }
    }
}

bool next_combination(std::vector<uint32_t> &combination, uint32_t n) {
    for (uint32_t i = 0; i < combination.size(); i++) {
        uint32_t upper_bound;
        if (i == combination.size() - 1) {
            upper_bound = n;
        } else {
            upper_bound = combination[i + 1];
        }
        if (++combination[i] == upper_bound) {
            combination[i] = i;
        } else {
            return true;
        }
    }
    return false;
}

unsigned int log(unsigned int k) {
    unsigned int i = 0;
    while (k > 0) {
        k /= 2;
        i++;
    }
    return i;
}
