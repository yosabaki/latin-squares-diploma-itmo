// -----------------------------------------------------------------------------
// Copyright (C) 2017  Ludovic LE FRIOUX
//
// This file is part of PaInleSS.
//
// PaInleSS is free software: you can redistribute it and/or modify it under the
// terms of the GNU General Public License as published by the Free Software
// Foundation, either version 3 of the License, or (at your option) any later
// version.
//
// This program is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
// details.
//
// You should have received a copy of the GNU General Public License along with
// this program.  If not, see <http://www.gnu.org/licenses/>.
// -----------------------------------------------------------------------------

#include "../working/DivideAndConquer.h"
#include "../utils/Logger.h"
#include "../utils/System.h"
#include "../utils/Parameters.h"
#include "../solvers/SolverFactory.h"
#include "../painless.h"

#include <unistd.h>
#include <algorithm>

#define TIME_BEFORE_INTERRUPTING 2 //in s


using namespace std;

template<typename T>
static bool getHeadFromVector(vector<T *> & vec, T ** ws)
{
   if(vec.size() == 0)
      return false;
   *ws = vec[0];
   vec.erase(vec.begin());

   return true;
}

template<typename T>
static bool removeFromVector(vector<T *> & vec, T * ws)
{
   for(int i = 0; i < vec.size(); i++) {
      if(vec[i] == ws) {
         vec.erase(vec.begin() + i);
         return true;
      }
   }

   return false;
}

template<typename T>
static bool addOnceToVectorTail(vector<T *> & vec, T * ws)
{
   for(int i = 0; i < vec.size(); i++) {
      if(vec[i] == ws)
         return false;
   }
   vec.push_back(ws);

   return true;
}

template<typename T>
static bool addOnceToVectorHead(vector<T *> & vec, T * ws)
{
   for(int i = 0; i < vec.size(); i++) {
      if(vec[i] == ws)
         return false;
   }
   vec.insert(vec.begin(), ws);

   return true;
}

template<typename T>
static bool doesVectorContain(vector<T *> & vec, T * ws)
{
   for(int i = 0; i < vec.size(); i++) {
      if(vec[i] == ws)
         return true;
   }

   return false;
}

// Copy every element from source to target
template<typename T>
static inline void mergeVector(vector<T>& source, vector<T> & target)
{
   target.insert(target.end(), source.begin(), source.end());
}


void * mainMasterDivideAndConquer(void * arg)
{
   DivideAndConquer * dc = (DivideAndConquer *)arg;

   pthread_mutex_lock(&dc->mutexStart); //Wait for the beginning.

   if(dc->waitJob == true) {
      log(0, "Thread Master is waiting for beginning.\n");
      pthread_cond_wait(&dc->mutexCondStart, &dc->mutexStart);
   }

   pthread_mutex_unlock(&dc->mutexStart);

   int size = dc->slaves.size();
   for(int i = 0; i < size; i++) {
      // Initially, every slave is waiting for a job.
      dc->overs.push_back(dc->slaves[i]);
      // Every slave has the same cube from master.
      dc->cubes[dc->slaves[i]] = vector<int>(dc->actualCube);
   }

   WorkingStrategy * actualWorker, * actualOver;

   getHeadFromVector(dc->overs, &actualWorker);
   dc->workers.push_back(actualWorker);

   dc->nWorkers++;
   dc->times[actualWorker] = getAbsoluteTime();
   actualWorker->solve(dc->cubes[actualWorker]); //We launch one SW

   log(0, "Master has started first worker and is now waiting and managing" \
       " requests.\n");

   while(globalEnding == false && dc->strategyEnding == false) {

      pthread_mutex_lock(&dc->mutexListsWorkers);

      while(dc->overs.empty() || dc->workers.empty()){
         log(2,"Thread Master is waiting for work.\n");
         pthread_cond_wait(&dc->mutexCondLists, &dc->mutexListsWorkers);
      }

      // If there is no longer workers, the problem is solved
      if(dc->nWorkers.load() == 0){
         pthread_mutex_unlock(&dc->mutexListsWorkers);
         break;
      }

      log(2, "Master has some work\n");
      actualWorker = dc->workers[0];

      double delta = dc->times[actualWorker] +
                     TIME_BEFORE_INTERRUPTING - getAbsoluteTime();

      // Workers are working for too short, master will wait until the
      // TIME_BEFORE_INTERRUPTING is reached.
      if(delta > 0){
         log(2, "Master: workers are working for too short,I will sleep %f " \
             " sec\n", delta);

         pthread_mutex_unlock(&dc->mutexListsWorkers);

         usleep((int)(delta * 1000000));
         continue;
      }

      getHeadFromVector(dc->overs,&actualOver);
      dc->workers.erase(dc->workers.begin());

      addOnceToVectorTail(dc->workersSplitting,actualWorker);
      addOnceToVectorTail(dc->oversSplitting,actualOver);

      log(2,"Master Interrupts %p and added %p to splitting list\n",
          actualWorker,actualOver);

      //Request the SW to stop but we don't wait for it.
      actualWorker->setInterrupt();

      pthread_mutex_unlock(&dc->mutexListsWorkers);
   }

   log(2, "Master Thread is done.\n");
   return NULL;
}

DivideAndConquer::DivideAndConquer()
{
   cloneStrategy    = Parameters::getIntParam("copy-mode", 1);
   divisionStrategy = Parameters::getIntParam("split-heur", 1);

   pthread_mutex_init(&mutexStart, NULL);
   pthread_mutex_init(&mutexListsWorkers,NULL);
   pthread_mutex_init(&mutexFirstSolution,NULL);

   pthread_cond_init(&mutexCondStart, NULL);
   pthread_cond_init(&mutexCondLists, NULL);

   strategyEnding = false;

   nWorkers            = 0;
   nDivisions          = 0;
   nCancelledDivisions = 0;

   waitJob = true;

   master = new Thread(mainMasterDivideAndConquer, this);
}

DivideAndConquer::~DivideAndConquer()
{
   master->join();
   delete master;

   for (int i = 0; i < slaves.size(); i++) {
      delete slaves[i];
   }

   pthread_mutex_destroy(&mutexStart);
   pthread_mutex_destroy(&mutexListsWorkers);
   pthread_mutex_destroy(&mutexFirstSolution);

   pthread_cond_destroy (&mutexCondStart);
   pthread_cond_destroy(&mutexCondLists);
}

void
DivideAndConquer::solve(const vector<int> & cube)
{
   actualCube=cube;

   waitJob = false;

   pthread_mutex_lock(&mutexStart);

   pthread_cond_signal(&mutexCondStart);

   pthread_mutex_unlock(&mutexStart);
}

void
DivideAndConquer::join(WorkingStrategy * current, SatResult res,
                       const vector<int> & model)
{
   //If res is UNKNOWN, this slave has been interrupted to split.
   if (res == UNKNOWN){
      nDivisions++;

      pthread_mutex_lock(&mutexListsWorkers);

      WorkingStrategy * over;
      getHeadFromVector(oversSplitting, &over);

      removeFromVector(workersSplitting, current);

      double divisionTime = getAbsoluteTime();

      SolverInterface * currentSolver = ((SequentialWorker *)current)->solver;
      SolverInterface * overSolver    = ((SequentialWorker *)over)->solver;

      // Create a new solver by copying the current one.
      if(cloneStrategy == 2) {
         SolverInterface * cloneSolver =
            SolverFactory::cloneSolver(currentSolver);

         if (sharers != NULL) {
            for (int i = 0; i < nSharers; i++) {
               sharers[i]->addConsumer(cloneSolver);
               sharers[i]->addProducer(cloneSolver);
            }
         }

         ((SequentialWorker *)over)->solver = cloneSolver;
      }

      int var = current->getDivisionVariable();
      splittingTimesLog.push_back(getAbsoluteTime() - divisionTime);

      log(1,"Slave %p is splitting for %p, division variable is: %d\n",
          current, over, var);

      //Copy the worker's cube to the over
      cubes[over] = vector<int>(cubes[current]);

      cubes[current].push_back(var);
      cubes[over].push_back(-var);

      workers.push_back(current);
      workers.push_back(over); //Both WS are now working.

      nWorkers++;

      double actualTime = getAbsoluteTime();
      times[current]    = actualTime;
      times[over]       = actualTime;

      current->solve(cubes[current]);
      over->solve(cubes[over]);

      if(!overs.empty()) {
         // Signal the Master that workers are available to split
         // (Only if overs are available).
         pthread_cond_signal(&mutexCondLists);
      }

      pthread_mutex_unlock(&mutexListsWorkers);

      return;
   }

   if(res == UNSAT){
      log(1, "An UNSAT sub-problem has been solved by %p\n", current);

      pthread_mutex_lock(&mutexListsWorkers);

      timesLog.push_back(getAbsoluteTime() - times[current]);

      // If current is in workersSplitting, it means that it was supposed to
      // split, but it has finished, so it has to cancel the division.
      if(doesVectorContain(workersSplitting, current)) {
         log(2, "Slave %p didn't split because it has finished\n", current);

         nCancelledDivisions++;

         WorkingStrategy * oldOver;
         getHeadFromVector(oversSplitting, &oldOver);
         addOnceToVectorHead(overs, oldOver);
         removeFromVector(workersSplitting, current);
      }else{
         removeFromVector(workers, current);
      }

      addOnceToVectorTail(overs, current);

      if(!workers.empty()) {
         // Signal the master that an over is available to work
         // (Only if workers are available to split).
         pthread_cond_signal(&mutexCondLists);
      }

      current->setInterrupt();

      nWorkers--;

      if(nWorkers > 0){
         if(cloneStrategy == 2) {
            SolverInterface * currentSolver =
               ((SequentialWorker *)current)->solver;
            ((SequentialWorker *)current)->solver = NULL;

            pthread_mutex_unlock(&mutexListsWorkers);

            if(currentSolver == NULL) {
               return;
            }

            if (sharers != NULL) {
               for (int i = 0; i < nSharers; i++) {
                  sharers[i]->removeConsumer(currentSolver);
                  sharers[i]->removeProducer(currentSolver);
               }
            }

            currentSolver->release();

            return;
         }

         pthread_mutex_unlock(&mutexListsWorkers);

         return;
      }

      pthread_mutex_unlock(&mutexListsWorkers);
   }

   pthread_mutex_lock(&mutexFirstSolution);

   if(strategyEnding == true) {
      pthread_mutex_unlock(&mutexFirstSolution);
      return;
   }

   strategyEnding = true;

   //Only the first slave has to go there, the others must just return.
   for (size_t i = 0; i < slaves.size(); i++) {
      slaves[i]->setInterrupt(); //Interrupting slaves.
   }

   pthread_mutex_unlock(&mutexFirstSolution);

   if(parent == NULL) {
      globalEnding = true;
      finalResult  = res;

      log(0, "DivideAndConquer (%p) found a solution\n", current);

      sort(timesLog.begin(), timesLog.end());

      log(1, "Number of divisions: %d, number of cancelled divisions: %d\n",
          nDivisions.load(), nCancelledDivisions.load());

      string logString="Times for each work:\nc ";
      for(int i = 0; i < timesLog.size(); i++) {
         if(i % 8 == 0 && i != timesLog.size() && i != 0) {
            logString.append("\nc ");
         }
         logString.append(to_string(timesLog[i]));
         logString.append(" ");
      }
      log(1,"%s\n",logString.c_str());

      logString = string("Divisions times:\nc ");
      for(int i = 0; i < splittingTimesLog.size(); i++) {
         if(i % 8 == 0 && i != splittingTimesLog.size() && i != 0) {
            logString.append("\nc ");
         }
         logString.append(to_string(splittingTimesLog[i]));
         logString.append(" ");
      }
      log(1, "%s\n", logString.c_str());

      if(res == SAT){
         finalModel= model;
      }

   }else{
      parent->join(this,res,model);
   }
}

void
DivideAndConquer::setInterrupt()
{
   for (size_t i = 0; i < slaves.size(); i++) {
      slaves[i]->setInterrupt();
   }

}

void
DivideAndConquer::unsetInterrupt()
{
   for (size_t i = 0; i < slaves.size(); i++) {
      slaves[i]->unsetInterrupt();
   }     
}

void
DivideAndConquer::waitInterrupt()
{
   for (size_t i = 0; i < slaves.size(); i++) {
      slaves[i]->waitInterrupt();
   }
}

//Not used actually
int
DivideAndConquer::getDivisionVariable()
{
   return 0;
}

//Not used actually
void
DivideAndConquer::setPhase(int var, bool value)
{
}

//Not used actually
void
DivideAndConquer::bumpVariableActivity(int var, int times)
{
}
