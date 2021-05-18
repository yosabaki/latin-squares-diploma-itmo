//
// Created by uWX962939 on 5/8/2021.
//

#include "Node.h"

bool ValueNode::propagate(const std::vector<uint8_t> &units, const std::vector<uint32_t> &flipped) const {
    return value;
}

ValueNode::ValueNode(int variable, bool value) : Node(variable, std::vector<Literal>()), value(value) {
    depth = 0;
}

AndIffNode::AndIffNode(int variable, std::vector<Literal> &&args) : Node(variable, std::move(args)) {}

bool AndIffNode::propagate(const std::vector<uint8_t> &units, const std::vector<uint32_t> &flipped) const {
    uint32_t i = 0;
    for (auto &arg: args) {
        while (i < flipped.size() && flipped[i] < arg.number) {
            i++;
        }
        bool isFlipped = false;
        if (i != flipped.size() && flipped[i] == arg.number) {
            isFlipped = true;
        }
        bool value = units[arg.number] ^isFlipped;
        if (value != arg.value) {
            return false;
        }
    }
    return true;
}

OrIffNode::OrIffNode(int variable, std::vector<Literal> &&args) : Node(variable, std::move(args)) {}

bool OrIffNode::propagate(const std::vector<uint8_t> &units, const std::vector<uint32_t> &flipped) const {
    int i = 0;
    for (auto &arg: args) {
        while (i < flipped.size() && flipped[i] < arg.number) {
            i++;
        }
        bool isFlipped = false;
        if (i != flipped.size() && flipped[i] == arg.number) {
            isFlipped = true;
        }
        int x = units[arg.number];
        bool value = units[arg.number] ^isFlipped;
        if (value == arg.value) {
            return true;
        }
    }
    return false;
}

bool OrAndNode::propagate(const std::vector<uint8_t> &units, const std::vector<uint32_t> &flipped) const {
    for (auto &args: orAndArgs) {
        bool flag = true;
        uint32_t i = 0;
        for (auto &arg: args) {
            bool isFlipped = false;
            if (i != flipped.size() && flipped[i] == arg.number) {
                isFlipped = true;
            }
            bool value = units[arg.number] ^isFlipped;
            if (value != arg.value) {
                flag = false;
                break;
            }
        }
        if (flag) {
            return true;
        }
    }
    return false;
}

OrAndNode::OrAndNode(int variable, std::vector<std::vector<Literal>> &&metaArgs) : Node(variable) {
    std::vector<Literal> constructor_args;
    for (auto &args: metaArgs) {
        for (auto &arg: args) {
            constructor_args.push_back(arg);
        }
    }
    orAndArgs = std::move(metaArgs);
    args = std::move(constructor_args);
}
