# cmake_minimum_required(VERSION <specify CMake version here>)
project(localsearch)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Wall -O3 -pthread")

add_executable(main
        main.cpp net/Net.cpp net/Net.h net/Node.cpp net/Node.h cnf/CNF.cpp cnf/CNF.h cnf/Literal.cpp cnf/Literal.h cnf/Clause.cpp cnf/Clause.h solver/HillClimbingSolver.cpp solver/HillClimbingSolver.h utils/utils.cpp utils/utils.h utils/thread_pool.hpp cnf/MetaVariable.cpp cnf/MetaVariable.h)