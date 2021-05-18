//
// Created by uWX962939 on 5/8/2021.
//

#ifndef METACDCL_NODE_H
#define METACDCL_NODE_H

#include <vector>
#include "../cnf/Literal.h"

struct Node {
    std::vector<Literal> args;
    uint32_t variable;
    int depth = -1;

    explicit Node(uint32_t variable) : variable(variable) {}

    Node(int variable, std::vector<Literal> &&args) : variable(variable), args(std::move(args)) {}

    virtual bool propagate(const std::vector<uint8_t> &units, const std::vector<uint32_t> &flipped) const = 0;

    virtual ~Node() = default;
};

struct ValueNode : virtual Node {
    bool value;

    ValueNode(int variable, bool value);

    bool propagate(const std::vector<uint8_t> &units, const std::vector<uint32_t> &flipped) const override;
};


struct AndIffNode : virtual Node {
    AndIffNode(int variable, std::vector<Literal> &&args);

    bool propagate(const std::vector<uint8_t> &units, const std::vector<uint32_t> &flipped) const override;
};

struct OrIffNode : virtual Node {
    OrIffNode(int variable, std::vector<Literal> &&args);

    bool propagate(const std::vector<uint8_t> &units, const std::vector<uint32_t> &flipped) const override;
};

struct OrAndNode : virtual Node {
    std::vector<std::vector<Literal>> orAndArgs;

    OrAndNode(int variable, std::vector<std::vector<Literal>> &&metaArgs);

    bool propagate(const std::vector<uint8_t> &units, const std::vector<uint32_t> &flipped) const override;
};


#endif //METACDCL_NODE_H
