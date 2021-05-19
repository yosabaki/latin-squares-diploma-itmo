#/usr/bin/bash
OBJS=("SolverConfiguration.o" "ParallelSolver.o" "ClausesBuffer.o" "SolverCompanion.o" "MultiSolvers.o" "Main.o" "SharedCompanion.o")
for i in ${!OBJS[@]}; do
	echo ${OBJS[$i]}
	until ar q lib_standard.a ${OBJ[$i]}; do :; done;
done;
