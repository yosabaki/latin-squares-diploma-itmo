//
// Created by uWX962939 on 5/8/2021.
//
#include <sstream>
#include <algorithm>
#include "Net.h"

void Net::parse_node(const std::string &str) {
    std::istringstream ss(str);
    std::string tag;
    ss >> tag; // 'c'
    ss >> tag;
    int variable;
    ss >> variable;
    if (variable >= variable_to_node.size()) {
        variable_to_node.resize(variable + 1, UINT32_MAX);
    }
    variable_to_node[variable] = nodes.size();
    if (tag == "andIff") {
        std::vector<Literal> args;
        Literal arg;
        while (ss >> arg) {
            args.push_back(arg);
        }
        nodes.push_back(std::make_unique<AndIffNode>(variable, std::move(args)));
    } else if (tag == "orIff") {
        std::vector<Literal> args;
        Literal arg;
        while (ss >> arg) {
            args.push_back(arg);
        }
        nodes.push_back(std::make_unique<OrIffNode>(variable, std::move(args)));
    } else if (tag == "or_and") {
        std::vector<std::vector<Literal>> args;
        int arg;
        std::vector<Literal> current_args;
        while (ss >> arg) {
            if (arg == 0) {
                std::vector<Literal> tmp_args;
                std::swap(tmp_args, current_args);
                args.push_back(std::move(tmp_args));
            } else {
                current_args.emplace_back(arg);
            }
        }
        nodes.push_back(std::make_unique<OrAndNode>(variable, std::move(args)));
    } else if (tag == "value") {
        std::string value;
        ss >> value;
        nodes.push_back(std::make_unique<ValueNode>(variable, value == "true"));
    }
}

void Net::finalize(const uint32_t &i) {
    int maxDepth = 0;
    for (auto &arg: nodes[i]->args) {
        uint32_t k = arg.number;
        int depth;
        uint32_t nodeIndex = variable_to_node[k];
        if (nodeIndex == UINT32_MAX) {
            depth = 0;
        } else {
            depth = nodes[nodeIndex]->depth;
        }
        if (depth == -1) {
            finalize(nodeIndex);
            depth = nodes[nodeIndex]->depth;
        }
        if (depth > maxDepth) {
            maxDepth = depth;
        }
    }
    nodes[i]->depth = maxDepth + 1;
}

void Net::finalize() {
    for (uint32_t i = 0; i < nodes.size(); i++) {
        if (nodes[i]->depth == -1) {
            finalize(i);
        }
    }
    std::sort(nodes.begin(), nodes.end(),
              [](const std::unique_ptr<Node> &a, const std::unique_ptr<Node> &b) -> bool {
                  return a->depth < b->depth;
              });
    for (uint32_t i = 0; i < nodes.size(); i++) {
        for (auto &arg: nodes[i]->args) {
            if (arg.number >= literal_to_nodes.size()) {
                literal_to_nodes.resize(arg.number + 1, std::vector<uint32_t>());
            }
            literal_to_nodes[arg.number].push_back(i);
        }
    }
    finalized.reserve(nodes.size());
    for (int i = 0; i < nodes.size(); i++) {
        finalized.push_back(std::move(nodes[i]));
    }
}

void Net::compute(std::vector<uint8_t> &units) {
    std::vector<uint32_t> flipped;
    for (auto &node : finalized) {
        auto variable = node->variable;
        if (units[variable] == 2) {
            units[variable] = node->propagate(units, flipped);
        }
    }
}

void Net::compute(const std::vector<uint8_t> &units, std::vector<uint32_t> &flipped) const {
    uint32_t i = 0;
    std::vector<uint8_t> toCompute(units.size(), false);
    while (i < flipped.size()) {
        if (literal_to_nodes.size() <= flipped[i]) {
            i++;
            continue;
        }
        for (uint32_t k: literal_to_nodes[flipped[i]]) {
            toCompute[finalized[k]->variable] = true;
        }
        i++;
    }
    for (auto &node : finalized) {
        uint32_t variable = node->variable;
        if (toCompute[variable]) {
            uint8_t current = units[variable];
            if (node->propagate(units, flipped) != current) {
                flipped.push_back(variable);
                uint32_t last = flipped.size() - 1;
                while (last > 0 && flipped[last] < flipped[last - 1]) {
                    std::swap(flipped[last], flipped[--last]);
                }
                for (uint32_t k: literal_to_nodes[variable]) {
                    toCompute[finalized[k]->variable] = true;
                }
            }
        }
    }
}
