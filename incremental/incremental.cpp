#include <cryptominisat5/cryptominisat.h>
#include <assert.h>
#include <vector>
#include <fstream>
#include <sstream>
#include <unistd.h>
#include <sys/stat.h>
#include <ctime>
#include <chrono>
using std::vector;
using namespace CMSat;

int literalCount = 0;

bool isPositive(int number) {
    return number > 0;
}

vector<vector<Lit>> parseClauses(const std::string &filename) {
    int clauseCount;
    std::string temp;
    std::ifstream fin(filename);
    std::string line = "c";
    while (line[0] == 'c') {
        std::getline(fin, line);
    }
    std::istringstream s(line);
    s >> temp >> temp >> literalCount >> clauseCount;
    vector<vector<Lit>> clauses(clauseCount);
    for (int i = 0; i < clauseCount; i++) {
        int literal;
        fin >> literal;
        while (literal != 0) {
            clauses[i].push_back(Lit(abs(literal) - 1, !isPositive(literal)));
            fin >> literal;
        }
    }
    fin.close();
    return clauses;
}

vector<Lit> parseLiterals(const std::string &filename) {
    int clauseCount;
    std::string temp;
    std::ifstream fin(filename);
    std::string line = "c";
    bool flag = false;
    std::cout << "incremental" << std::endl;
    while (line[0] == 'c') {
        std::getline(fin, line);
        if (flag) {
            std::istringstream s(line);
            s >> temp;
            vector<Lit> literals;
            int literal;
            while (s >> literal) {
                literals.push_back(Lit(abs(literal) - 1, isPositive(literal)));
                std::cout << literals.back() << std::endl;
            }
            fin.close();
            return literals;
        }
        if (line.substr(2, line.size()) == "incrementalVariables:") {
            flag = true;
        }
    } 
    fin.close();
    return vector<Lit>();
}

std::string buildString(const std::string &prefix, const std::string &main, int index) {
    std::ostringstream s;
    s << prefix << main << index;
    return s.str();
}

bool fileExists(const std::string &filename) {
    struct stat buffer;
    return (stat (filename.c_str(), &buffer) == 0);
}

int main(int argc, char *argv[]) {
    SATSolver solver;
    vector<Lit> clause;

    if (argc < 3 || argc > 4) {
        std::cout << "Usage: incremental [$inputfile] [$outputfile] [$threadCount = 4]\n";
        return 1;
    }
    std::string inputfile = argv[1];
    std::string outputfile = argv[2];
    uint32_t threadCount = 4;
    if (argc == 4) {
        threadCount = atoi(argv[3]);
    }
    solver.set_num_threads(threadCount);
    std::cout << "Reading clauses\n";
    vector<vector<Lit>> mainClauses = parseClauses(inputfile);
    vector<Lit> additionalClauses = parseLiterals(inputfile);

    //We need 3 variables. They will be: 0,1,2
    //Variable numbers are always trivially increasing
    solver.new_vars(literalCount);
    std::cout << literalCount << ' ' << mainClauses.size() << std::endl;


    for (auto &clause: mainClauses) {
        solver.add_clause(clause);
    }

    auto ret = solver.solve();

    auto begin = std::chrono::system_clock::now();
    std::cout << additionalClauses.size() << '\n';
    for (int i = 0; i < additionalClauses.size(); i++) {
        std::cout << "try decomposed " << additionalClauses[i] << std::endl;
        solver.add_clause(vector<Lit> { additionalClauses[i] });
        auto start = std::chrono::system_clock::now();
        ret = solver.solve();
        auto end = std::chrono::system_clock::now();
        if (ret == l_True) {
            std::chrono::duration<double> elapsed_time = end - start;
            std::chrono::duration<double> overall_time = end - begin;
            std::time_t end_time = std::chrono::system_clock::to_time_t(end);
            std::cout << "result " << i << " is sat" << std::endl;
            std::cout << "finished at " << std::ctime(&end_time) << std::endl;
            std::cout << "overall: " << overall_time.count() << ' ' << elapsed_time.count() << std::endl;
            std::ofstream fout(buildString(outputfile, "_", i));
            fout<<"s SATISFIABLE" << std::endl;
            fout<<"v ";
            auto &model = solver.get_model();
            for (int i = 0; i < model.size(); i++) {
                int literal = (i + 1) * (model[i] == l_True ? 1 : -1);
                fout << literal << ' ';
            }
            fout.close();
        } else {
            std::cout << "result " << i << " is unsat";
            return 0;
        }
    }
    return 0;
}
