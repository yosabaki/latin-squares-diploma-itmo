//
// Created by uWX962939 on 5/8/2021.
//

#ifndef METACDCL_NET_H
#define METACDCL_NET_H

#include <vector>
#include <map>
#include <memory>
#include "Node.h"


struct Net {
    std::vector<std::unique_ptr<Node>> nodes;
    std::vector<std::unique_ptr<const Node>> finalized;
    std::vector<std::vector<uint32_t>> literal_to_nodes;
    std::vector<uint32_t> variable_to_node;

    Net() = default;

    Net(Net &&other) noexcept:
            nodes(std::move(other.nodes)),
            finalized(std::move(other.finalized)),
            literal_to_nodes(std::move(other.literal_to_nodes)),
            variable_to_node(std::move(other.variable_to_node)) {}

    void parse_node(const std::string &str);

    void finalize();

    void compute(std::vector<uint8_t> &units);

    void compute(const std::vector<uint8_t> &units, std::vector<uint32_t> &flipped) const;

    ~Net() = default;

private:
    void finalize(const uint32_t &i);
};


#endif //METACDCL_NET_H
