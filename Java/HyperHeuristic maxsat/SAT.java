package SAT;



import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;


public class SAT
{
	private final ParametreHeuristique[] Gsat_Parametres = {new ParametreHeuristique(10,50,-1), new ParametreHeuristique(20,40,-1)
			,new ParametreHeuristique(10,60,-1),new ParametreHeuristique(10,70,-1),new ParametreHeuristique(10,80,-1)};
	private final ParametreHeuristique[] Hsat_Parametres = {new ParametreHeuristique(10,100,-1),new ParametreHeuristique(10,10,-1),
			new ParametreHeuristique(10,60,-1),new ParametreHeuristique(10,70,-1),new ParametreHeuristique(10,80,-1)  };
	private final ParametreHeuristique[] Walk_Parametres = { new ParametreHeuristique(10,100,0.4),new ParametreHeuristique(10,10,0.2)
			,new ParametreHeuristique(10,135,0.5),new ParametreHeuristique(10,20,0.1),new ParametreHeuristique(10,50,0.5)};
	private final ParametreHeuristique[] Novelty_Parametres = {new ParametreHeuristique(10,100,0.6),new ParametreHeuristique(10,10,0.8)
			,new ParametreHeuristique(10,60,0.95),new ParametreHeuristique(10,130,0.1),new ParametreHeuristique(10,50,0.5)};
	private final ParametreHeuristique[] RLRAndomFlip_Parametres = {new ParametreHeuristique(10,30,-1), new ParametreHeuristique(10,10,-1)
			,new ParametreHeuristique(10,60,-1),new ParametreHeuristique(10,70,-1),new ParametreHeuristique(10,80,-1)};
	private final ParametreHeuristique[] RLRAndomFlipBc_Parametres = {new ParametreHeuristique(10,30,-1), new ParametreHeuristique(10,10,-1)
			,new ParametreHeuristique(10,60,-1),new ParametreHeuristique(10,70,-1),new ParametreHeuristique(10,80,-1)};
  public Random rng;
  public double[][] heuristicMeanRecord;
  public int numberOfClauses;
  public int numberOfVariables;
  public Clause[] clauses;
  public Solution[] solutionMemory;
  public int bestEverHeuristic;
  public final int number_of_heuristics=6;
  public final int number_of_sub_heuristics=5;
  public double bestEverMean = Double.POSITIVE_INFINITY;

  
  public SAT(long seed)
  {
	  this.heuristicMeanRecord = new double[number_of_heuristics][number_of_sub_heuristics];
	   // this.heuristicCallTimeRecord = new int[number_of_heuristics];
	    if (seed == -1) {
	      this.rng = new Random();
	    } else {
	      this.rng = new Random(seed);
	    }
  }
  
 
  private LinkedList<Integer> getVariablesWithHighestNetGain(Solution tempSolution)
  {
    int[] numbersofbrokenclauses = new int[this.numberOfVariables];
    for (int x = 0; x < this.numberOfVariables; x++) {
      numbersofbrokenclauses[x] = tempSolution.testFlipForBrokenClauses(x);
    }
    int minimum = numbersofbrokenclauses[0];
    for (int i = 0; i < numbersofbrokenclauses.length; i++) {
      if (numbersofbrokenclauses[i] < minimum) {
        minimum = numbersofbrokenclauses[i];
      }
    }
    LinkedList<Integer> jointminimums = new LinkedList<Integer>();
    for (int i = 0; i < this.numberOfVariables; i++) {
      if (numbersofbrokenclauses[i] == minimum) {
        jointminimums.add(new Integer(i));
      }
    }
    return jointminimums;
  }
  
  
  public int[] generateNeighbor(int i,int nbH,int nbSH){
      // nbH : nombre d heuristiques
      //nbSH : nombre de sous heuristiques de chaque heuristique
      int [] tab = {0,0,0,0};
      if (i>=11 && i<=99){
          int a = i % 10;
          int b = i / 10;

          int aa,bb;

          //voisin 1
          aa = a+1;
          if (aa == nbSH+1) aa=1;
          bb=b;
          tab[0] = bb*10+aa;



          //voisin 2
          aa = a-1;
          if (aa == 0) aa=nbSH;
          bb=b;
          tab[1] = bb*10+aa;
          //voisin 3
          aa = a;
          bb=b+1;
          if (bb == nbH+1) bb=1;
          tab[2] = bb*10+aa;

          //voisin 4
          aa = a;
          bb=b-1;
          if (bb == 0) bb=nbH;
          tab[3] = bb*10+aa;


      }
      return tab;

  }
  public void HyperHeuristic (int MaxIter,int SizeListeTabou,int nbExecution )
  {
	  Tabou<Integer> tabou= new Tabou<Integer>(SizeListeTabou);
	  int neighbors[],index_heuristic;
	  int heuristic_to_apply = rng.nextInt(number_of_heuristics)+1;
	  int sub_heuristic_to_apply = rng.nextInt(number_of_sub_heuristics)+1;
	  bestEverMean= applyHeuristic( heuristic_to_apply,sub_heuristic_to_apply,nbExecution, 0);
	  index_heuristic=sub_heuristic_to_apply+10*heuristic_to_apply;
	  tabou.add(index_heuristic);
	  bestEverHeuristic=index_heuristic;
	  
	  for (int i=0;i<MaxIter;i++)
	  {
		  neighbors=generateNeighbor(index_heuristic, number_of_heuristics, number_of_sub_heuristics);
		 // System.out.println(index_heuristic);
		  double neighborMeanMin=Double.POSITIVE_INFINITY;
		 // System.out.println(tabou.element());
		  for (int k=0;k<neighbors.length;k++)
		  {
			if (!tabou.contains(neighbors[k]))
			{
			  double tempMean=applyHeuristic(neighbors[k]/10,neighbors[k]%10,nbExecution, 1); 
			  
			 // System.out.println("neigh :"+tempMean+" "+neighbors[k]);
			  if (tempMean<neighborMeanMin)
			  {
				  neighborMeanMin=tempMean;	
				  index_heuristic=neighbors[k];
			  }	   
		  }
		  }
		  tabou.add(index_heuristic);
		  if (neighborMeanMin<bestEverMean  )
		  {
			  bestEverMean=neighborMeanMin;
			  bestEverHeuristic=index_heuristic; 
		  } 
	  }
  }
  /********************************les heuristiques****************************************************/
  //GSAT
  /*
   * Flip the variable with the highest net gain, and break ties randomly.
   */
  public void applyHeuristic0(Solution tempSolution,int maxEssai)
  {
	  
    for (int re = 0; re < maxEssai; re++)
    {
    	
      LinkedList<Integer> highestNetGains = getVariablesWithHighestNetGain(tempSolution);
      Integer i = (Integer)highestNetGains.get(this.rng.nextInt(highestNetGains.size()));
      tempSolution.variables[i.intValue()].permanentflip();
    }
  }
  
  public double  Gsat (int MaxInvers,int MaxEssai,int index)
  {
	  this.solutionMemory[index] = new Solution();
	  Solution temporarySolution = new Solution();
	  double bestObjectiveFunction = Double.POSITIVE_INFINITY;
	  
	 for (int k=0;k<MaxInvers;k++) 
	//	 IntStream.range(0,MaxInvers).parallel().forEach(i->{
			 {    
		 //intialisation de la sol
		    for (int y = 0; y < numberOfVariables; y++) {
		    	temporarySolution.variables[y].state = this.rng.nextBoolean();
		    }
		    applyHeuristic0(temporarySolution,MaxEssai);
		    double currentObjectiveFunction =evaluateObjectiveFunction(temporarySolution);
		   // System.out.println(currentObjectiveFunction);
		    if (currentObjectiveFunction<bestObjectiveFunction)
		    {
		    	 this.solutionMemory[index] = deepCopyTheSolution(temporarySolution);
		    	 bestObjectiveFunction=currentObjectiveFunction;
		    } 
		    if (bestObjectiveFunction==0) break;
			 }
		 //});
	 return bestObjectiveFunction;
  }
  
  
  
  //HSAT
  /*
   * Identical functionality to GSAT, but ties are broken by selecting the variable
	with the highest age.
   */
  public void applyHeuristic1(Solution tempSolution,int maxEssai)
  {
    for (int re = 0; re < maxEssai; re++)
    {
      LinkedList<Integer> jointminimums = getVariablesWithHighestNetGain(tempSolution);
      Variable largestage = tempSolution.variables[((Integer)jointminimums.getFirst()).intValue()];
      for (int x = 0; x < jointminimums.size(); x++)
      {
        Variable contender = tempSolution.variables[((Integer)jointminimums.get(x)).intValue()];
        if (contender.age > largestage.age) {
          largestage = contender;
        }
      }
      largestage.permanentflip();
    }
  }
  
  public double Hsat (int MaxInvers,int MaxEssai,int index)
  {
	  this.solutionMemory[index] = new Solution();
	  Solution temporarySolution = new Solution();
	  double bestObjectiveFunction = Double.POSITIVE_INFINITY; ;
	 for (int k=0;k<MaxInvers;k++) 
	 {
		 //intialisation de la sol
		    for (int y = 0; y < numberOfVariables; y++) {
		    	temporarySolution.variables[y].state = this.rng.nextBoolean();
		    }
		    applyHeuristic1(temporarySolution,MaxEssai);
		    double currentObjectiveFunction =evaluateObjectiveFunction(temporarySolution);
		    if (currentObjectiveFunction<bestObjectiveFunction)
		    {
		    	 this.solutionMemory[index] = deepCopyTheSolution(temporarySolution);
		    	 bestObjectiveFunction=currentObjectiveFunction;
		    } 
		    if (bestObjectiveFunction==0) break;
	 }
	 return bestObjectiveFunction;
  }
  
  private Clause getRandomBrokenClause(Solution tempSolution)
  {
    Vector<Clause> brokenClauses = new Vector<Clause>();
    for (int x = 0; x < this.numberOfClauses; x++) {
      if (!this.clauses[x].evaluate(tempSolution.variables)) {
        brokenClauses.add(this.clauses[x]);
      }
    }
    if (brokenClauses.isEmpty()) {
      return null;
    }
    return (Clause)brokenClauses.get(this.rng.nextInt(brokenClauses.size()));
  }
  
  /*private void flipRandomVariableInClause(Solution tempSolution, Clause c)
  {
    int variable = this.rng.nextInt(c.numberOfVariables());
    int specificVariableNumber = c.variablenumbers[variable];
    tempSolution.variables[specificVariableNumber].permanentflip();
  }
  */
 /* private void flipRandomVariableInRandomBrokenClause(Solution tempSolution)
  {
    Clause randomBrokenClause = getRandomBrokenClause(tempSolution);
    if (randomBrokenClause == null) {
      return;
    }
    flipRandomVariableInClause(tempSolution, randomBrokenClause);
  }*/
  
  private int getNegativeGain(Solution tempSolution, int variableToFlip)
  {
    int numberNotNowSatisfied = tempSolution.testFlipForNegGain(variableToFlip);
    return numberNotNowSatisfied;
  }
  
  //WalkSAT
  /*
   * Select a random broken clause BC. If any variables in BC have a negative gain
	of zero, randomly select one of these to 
	ip. If no such variable exists, 
	ip a
	random variable in BC with probability 0.5, otherwise 
	ip the variable with
	minimal negative gain.
   */
  

  public void applyHeuristic2(Solution tempSolution,int MaxEssai,double walkProba)
  {
	  
    for (int re = 0; re < MaxEssai; re++)
    {
      Clause randomBrokenClause = getRandomBrokenClause(tempSolution);
      if (randomBrokenClause == null) {
        break;
      }
      int[] negativeGains = new int[randomBrokenClause.numberOfVariables()];
      
      Vector<Integer> variablesWithNegativeGain0 = new Vector<Integer>(randomBrokenClause.numberOfVariables());
      for (int x = 0; x < randomBrokenClause.numberOfVariables(); x++)
      {
        negativeGains[x] = getNegativeGain(tempSolution, randomBrokenClause.variablenumbers[x]);
        if (negativeGains[x] == 0) {
          variablesWithNegativeGain0.add(new Integer(randomBrokenClause.variablenumbers[x]));
        }
      }
      if (!variablesWithNegativeGain0.isEmpty())
      {
        int r = this.rng.nextInt(variablesWithNegativeGain0.size());
        int varnumber = ((Integer)variablesWithNegativeGain0.get(r)).intValue();
        
        tempSolution.variables[varnumber].permanentflip();
      }
      else  if (this.rng.nextDouble()<walkProba)
          {
    	  int i=this.rng.nextInt(randomBrokenClause.numberOfVariables());
            tempSolution.variables[randomBrokenClause.variablenumbers[i]].permanentflip();
          } else
          {
        int minimum = negativeGains[0];
        for (int x = 1; x < randomBrokenClause.numberOfVariables(); x++) {
          if (negativeGains[x] < minimum) {
            minimum = negativeGains[x];
          }
        }
        Vector<Integer> jointminimums = new Vector<Integer>(randomBrokenClause.numberOfVariables());
        for (int i = 0; i < randomBrokenClause.numberOfVariables(); i++) {
          if (negativeGains[i] == minimum) {
            jointminimums.add(new Integer(randomBrokenClause.variablenumbers[i]));
          }
        }
        tempSolution.variables[((Integer)jointminimums.get(this.rng.nextInt(jointminimums.size()))).intValue()].permanentflip();
      }
      }
    
  }
  
  public double Walksat (int MaxInvers,int MaxEssai,double walkProba,int index)
  {
	  this.solutionMemory[index] = new Solution();
	  Solution temporarySolution = new Solution();
	  double bestObjectiveFunction = Double.POSITIVE_INFINITY; 
	 
	 for (int k=0;k<MaxInvers;k++) 
	 {
		 //intialisation de la sol
		 for (int y = 0; y < numberOfVariables; y++) {
		    	temporarySolution.variables[y].state = this.rng.nextBoolean();
		    }
		    applyHeuristic2(temporarySolution,MaxEssai,walkProba);
		    double currentObjectiveFunction =evaluateObjectiveFunction(temporarySolution);
		    if (currentObjectiveFunction<bestObjectiveFunction)
		    {
		    	 this.solutionMemory[index] = deepCopyTheSolution(temporarySolution);
		    	 bestObjectiveFunction=currentObjectiveFunction;
		    } 
		    if (bestObjectiveFunction==0) break;
	 }
	 return bestObjectiveFunction;
  }
  
  //Flip a randomly selected variable from a randomly selected broken clause.
  /*public void applyHeuristic3(Solution tempSolution) //
  {
    for (int r = 0; r < this.mrepeats; r++) {
      flipRandomVariableInRandomBrokenClause(tempSolution);
    }
  }
  */
  
//Flip a variable selected completely at random.
  /*public void applyHeuristic4(Solution tempSolution)    
  {
    for (int r = 0; r < this.mrepeats; r++) {
      tempSolution.variables[this.rng.nextInt(this.numberOfVariables)].permanentflip();
    }
  }*/
  
  
  
  //heurisitque novelty
  /*
	Select a random broken clause BC. Flip the variable v with the highest net
gain, unless v has the minimal age in BC. If this is the case, then 
ip it with
0.3 probability. Otherwise 
ip the variable with the second highest net gain.
   */
  
  public double Novelty (int MaxInvers,int MaxEssai,double Proba,int index)
  {
	  this.solutionMemory[index] = new Solution();
	  Solution temporarySolution = new Solution();
	  double bestObjectiveFunction = Double.POSITIVE_INFINITY; 
	 
	 for (int k=0;k<MaxInvers;k++) 
	 {
		 //intialisation de la sol
		 for (int y = 0; y < numberOfVariables; y++) {
		    	temporarySolution.variables[y].state = this.rng.nextBoolean();
		    }
		    applyHeuristic5(temporarySolution,MaxEssai,Proba);
		    double currentObjectiveFunction =evaluateObjectiveFunction(temporarySolution);
		    if (currentObjectiveFunction<bestObjectiveFunction)
		    {
		    	 this.solutionMemory[index] = deepCopyTheSolution(temporarySolution);
		    	 bestObjectiveFunction=currentObjectiveFunction;
		    } 
		    if (bestObjectiveFunction==0) break;
	 }
	 return bestObjectiveFunction;
  }
  public void applyHeuristic5(Solution tempSolution,int maxEssai,double proba)
  {
    for (int re = 0; re < maxEssai; re++)
    {
      Clause randomBrokenClause = getRandomBrokenClause(tempSolution);
      if (randomBrokenClause == null) {
        break;
      }
      applyNovelty(tempSolution, randomBrokenClause,proba);
    }
  }
  
  
  
  private void applyNovelty(Solution tempSolution, Clause randomBrokenClause,double proba)
  {
   
    int[] numbersofbrokenclauses = new int[randomBrokenClause.numberOfVariables()];
    
    int minimalage = Integer.MAX_VALUE;
    for (int x = 0; x < randomBrokenClause.numberOfVariables(); x++)
    {
      numbersofbrokenclauses[x] = tempSolution.testFlipForBrokenClauses(randomBrokenClause.variablenumbers[x]);
      if (tempSolution.variables[randomBrokenClause.variablenumbers[x]].age < minimalage) {
        minimalage = tempSolution.variables[randomBrokenClause.variablenumbers[x]].age;
      }
    }
    int minimum = Integer.MAX_VALUE;
    int secondminimum = Integer.MAX_VALUE;
    for (int i = 0; i < randomBrokenClause.numberOfVariables(); i++) {
      if (numbersofbrokenclauses[i] < minimum)
      {
        secondminimum = minimum;
        minimum = numbersofbrokenclauses[i];
      }
      else if (numbersofbrokenclauses[i] < secondminimum)
      {
        secondminimum = numbersofbrokenclauses[i];
      }
    }
    Vector<Integer> jointminimums = new Vector<Integer>(randomBrokenClause.numberOfVariables());
    for (int i = 0; i < randomBrokenClause.numberOfVariables(); i++) {
      if (numbersofbrokenclauses[i] == minimum) {
        jointminimums.add(new Integer(i));
      }
    }
    Integer i = (Integer)jointminimums.get(this.rng.nextInt(jointminimums.size()));
    if (tempSolution.variables[randomBrokenClause.variablenumbers[i.intValue()]].age == minimalage)
    {
      if (this.rng.nextDouble()<proba)
      {
        tempSolution.variables[randomBrokenClause.variablenumbers[i.intValue()]].permanentflip();
      }
      else if (randomBrokenClause.numberOfVariables() == 1)
      {
        tempSolution.variables[randomBrokenClause.variablenumbers[i.intValue()]].permanentflip();
      }
      else if (minimum == secondminimum)
      {
        Integer q;
        do
        {
          q = (Integer)jointminimums.get(this.rng.nextInt(jointminimums.size()));
        } while (q.intValue() == i.intValue());
        tempSolution.variables[randomBrokenClause.variablenumbers[q.intValue()]].permanentflip();
      }
      else
      {
        jointminimums = new Vector<Integer>(randomBrokenClause.numberOfVariables());
        for (int q = 0; q < randomBrokenClause.numberOfVariables(); q++) {
          if (numbersofbrokenclauses[q] == secondminimum) {
            jointminimums.add(new Integer(q));
          }
        }
        if (jointminimums.isEmpty()) {
          System.out.println(minimum + " " + secondminimum + " " + randomBrokenClause.numberOfVariables());
        }
        Integer q = (Integer)jointminimums.get(this.rng.nextInt(jointminimums.size()));
        
        tempSolution.variables[randomBrokenClause.variablenumbers[q.intValue()]].permanentflip();
      }
    }
    else {
      tempSolution.variables[randomBrokenClause.variablenumbers[i.intValue()]].permanentflip();
    }
  }
  /***********************************Ruin-Recreate heuristics*******************************************/
  //Reinitialise Variables
 /* A proportion of the variables is randomly reinitialised. Depending on the value
  of the \intensity of mutation" parameter, either 0.2, 0.4, 0.6, or 0.8 of the
  solution is reinitialised.*/
  /*public void applyHeuristic6(Solution tempSolution)
  {
    double prop;
   // double prop;
    if (this.intensityOfMutation <= 0.25D)
    {
      prop = 0.2D;
    }
    else
    {
     // double prop;
      if (this.intensityOfMutation <= 0.49D)
      {
        prop = 0.4D;
      }
      else
      {
       // double prop;
        if (this.intensityOfMutation <= 0.75D) {
          prop = 0.6D;
        } else {
          prop = 0.8D;
        }
      }
    }
    int numofvariables = (int)(this.numberOfVariables * prop);
    
    int[] variables_to_reinitialise = new int[numofvariables];
   
    int count = 0;
    
    while (count < numofvariables)
    {
      int chosen = this.rng.nextInt(this.numberOfVariables);
      
      boolean alreadychosen = false;
     
      for (int y = 0; y < count; y++) {
        if (variables_to_reinitialise[y] == chosen)
        {
          alreadychosen = true;
          break;
        }
      }
      
      if (!alreadychosen)
      {
        variables_to_reinitialise[count] = chosen;
        count++;
      }
    }
   
    int[] arrayOfInt1;
    int y = (  arrayOfInt1 = variables_to_reinitialise).length;
    for (int alreadychosen = 0; alreadychosen < y; alreadychosen++)
    {
      
      if (this.rng.nextBoolean()) {
        tempSolution.variables[arrayOfInt1[alreadychosen]].permanentflip();
      }
    }
  }*/
  
  /*********************************Local searchers**********************************************/
	  /*These heuristics implement `rst-improvement' local search operators. In each
	iteration, a neighbour is generated, and it is accepted immediately if it has
	superior or equal tness. If the neighbour is worse, then the change is not
	accepted.*/


  //Flip Random Variable from a Broken Clause
  /*
   * Flip a randomly selected variable from a randomly selected broken clause.
   * */
  
  public double RLRAndomFlipBc (int MaxInvers,int MaxEssai,int index)
  {
	  this.solutionMemory[index] = new Solution();
	  Solution temporarySolution = new Solution();
	  double bestObjectiveFunction = Double.POSITIVE_INFINITY; ;
	 for (int k=0;k<MaxInvers;k++) 
	 {
		 //intialisation de la sol
		    for (int y = 0; y < numberOfVariables; y++) {
		    	temporarySolution.variables[y].state = this.rng.nextBoolean();
		    }
		    applyHeuristic7(temporarySolution,MaxEssai);
		    double currentObjectiveFunction =evaluateObjectiveFunction(temporarySolution);
		    if (currentObjectiveFunction<bestObjectiveFunction)
		    {
		    	 this.solutionMemory[index] = deepCopyTheSolution(temporarySolution);
		    	 bestObjectiveFunction=currentObjectiveFunction;
		    } 
		    if (bestObjectiveFunction==0) break;
	 }
	 return bestObjectiveFunction;
  }
  
  public void applyHeuristic7(Solution tempSolution,int MaxEssai)
  {
    double currentres = evaluateObjectiveFunction(tempSolution);
    for (int r = 0; r < MaxEssai; r++)
    {
      Clause randomBrokenClause = getRandomBrokenClause(tempSolution);
      if (randomBrokenClause == null) {
        break;
      }
      int variable = this.rng.nextInt(randomBrokenClause.numberOfVariables());
      int specificVariableNumber = randomBrokenClause.variablenumbers[variable];
      tempSolution.variables[specificVariableNumber].testflip();
      double res = evaluateObjectiveFunction(tempSolution);
      tempSolution.variables[specificVariableNumber].testflip();
      if (res <= currentres)
      {
        tempSolution.variables[specificVariableNumber].permanentflip();
        currentres = res;
      }
    }
  }
  
  //Flip Random Variable
  /*
   * Flip a variable selected completely at random.
   */
  public double RLRAndomFlip (int MaxInvers,int MaxEssai,int index)
  {
	  this.solutionMemory[index] = new Solution();
	  Solution temporarySolution = new Solution();
	  double bestObjectiveFunction = Double.POSITIVE_INFINITY; ;
	 for (int k=0;k<MaxInvers;k++) 
	 {
		 //intialisation de la sol
		    for (int y = 0; y < numberOfVariables; y++) {
		    	temporarySolution.variables[y].state = this.rng.nextBoolean();
		    }
		    applyHeuristic8(temporarySolution,MaxEssai);
		    double currentObjectiveFunction =evaluateObjectiveFunction(temporarySolution);
		    if (currentObjectiveFunction<bestObjectiveFunction)
		    {
		    	 this.solutionMemory[index] = deepCopyTheSolution(temporarySolution);
		    	 bestObjectiveFunction=currentObjectiveFunction;
		    } 
		    if (bestObjectiveFunction==0) break;
	 }
	 return bestObjectiveFunction;
  }
  public void applyHeuristic8(Solution tempSolution,int MaxEssai)
  {
    double currentres = evaluateObjectiveFunction(tempSolution);
    for (int r = 0; r < MaxEssai; r++)
    {
      int vtoflip = this.rng.nextInt(this.numberOfVariables);
      tempSolution.variables[vtoflip].testflip();
      double res = evaluateObjectiveFunction(tempSolution);
      tempSolution.variables[vtoflip].testflip();
      if (res <= currentres)
      {
        tempSolution.variables[vtoflip].permanentflip();
        currentres = res;
      }
    }
  }
  
  /*********************************Crossover heuristics********************************************/
  //Two point crossover
  /*
   * Standard two point crossover on the boolean strings of variables.
   */
  /*private void applyHeuristic9(Solution tempSolution1, Solution tempSolution2)
  {
    int crossoverpoint1 = this.rng.nextInt(tempSolution1.variables.length);
    int crossoverpoint2 = this.rng.nextInt(tempSolution1.variables.length);
    if (crossoverpoint1 > crossoverpoint2)
    {
      int temp = crossoverpoint1;crossoverpoint1 = crossoverpoint2;crossoverpoint2 = temp;
    }
    for (int x = crossoverpoint1; x < crossoverpoint2; x++) {
      tempSolution1.variables[x] = tempSolution2.variables[x];
    }
  }
  */
  //One point crossover
  /*
   * Standard one point crossover on the boolean strings of variables.
   */
  /*private void applyHeuristic10(Solution tempSolution1, Solution tempSolution2)
  {
    int crossoverpoint1 = this.rng.nextInt(tempSolution1.variables.length);
    for (int x = crossoverpoint1; x < tempSolution1.variables.length; x++) {
      tempSolution1.variables[x] = tempSolution2.variables[x];
    }
  }*/
  /****************************************fin des heuristiques**********************************************************/
 
  
  public double applyHeuristic(int heuristicID,int subHeuristicID,int nbExecution, int solutionSourceIndex)
  {
    //long startTime = System.currentTimeMillis();
   // Solution temporarysolution = deepCopyTheSolution(this.solutionMemory[solutionSourceIndex]);
  
    int MaxInvers=0,MaxEssai=0;
    double proba=-1;
    double objectiveFunction=Double.POSITIVE_INFINITY;
    double meanTemporary=0;
     for (int i=0;i<nbExecution;i++)
    {
    	
      if (heuristicID == 1)
      {
    	 MaxInvers=this.Gsat_Parametres[subHeuristicID-1].maxInvers;
    	 MaxEssai=this.Gsat_Parametres[subHeuristicID-1].maxEssai;
    	 proba=this.Gsat_Parametres[subHeuristicID-1].proba;
    	 objectiveFunction=Gsat(MaxInvers,MaxEssai,solutionSourceIndex);
      }
      else if (heuristicID == 2)
      {
    	  MaxInvers=this.Hsat_Parametres[subHeuristicID-1].maxInvers;
     	 MaxEssai=this.Hsat_Parametres[subHeuristicID-1].maxEssai;
     	 proba=this.Hsat_Parametres[subHeuristicID-1].proba;
     	objectiveFunction=Hsat(MaxInvers,MaxEssai,solutionSourceIndex);
      }
      else if (heuristicID == 3)
      {
    	  MaxInvers=this.Walk_Parametres[subHeuristicID-1].maxInvers;
      	 MaxEssai=this.Walk_Parametres[subHeuristicID-1].maxEssai;
      	 proba=this.Walk_Parametres[subHeuristicID-1].proba;
      	objectiveFunction=Walksat(MaxInvers,MaxEssai,proba,solutionSourceIndex);
      }
      else if (heuristicID == 4)
      {
    	  MaxInvers=this.Novelty_Parametres[subHeuristicID-1].maxInvers;
       	 MaxEssai=this.Novelty_Parametres[subHeuristicID-1].maxEssai;
       	 proba=this.Novelty_Parametres[subHeuristicID-1].proba;
       	objectiveFunction= Novelty(MaxInvers,MaxEssai,proba,solutionSourceIndex);
      }
      else if (heuristicID == 5)
      {
    	  MaxInvers=this.RLRAndomFlip_Parametres[subHeuristicID-1].maxInvers;
      	 MaxEssai=this.RLRAndomFlip_Parametres[subHeuristicID-1].maxEssai;
      	 proba=this.RLRAndomFlip_Parametres[subHeuristicID-1].proba;
      	objectiveFunction=RLRAndomFlip(MaxInvers,MaxEssai,solutionSourceIndex);
      }
      else if (heuristicID == 6)
      {
    	  MaxInvers=this.RLRAndomFlipBc_Parametres[subHeuristicID-1].maxInvers;
       	 MaxEssai=this.RLRAndomFlipBc_Parametres[subHeuristicID-1].maxEssai;
       	 proba=this.RLRAndomFlipBc_Parametres[subHeuristicID-1].proba;
       	objectiveFunction=RLRAndomFlipBc(MaxInvers,MaxEssai,solutionSourceIndex);
      }
    /*  else if (heuristicID == 6)
      {
        applyHeuristic6(temporarysolution);
      }
      else if (heuristicID == 7)
      {
       // applyHeuristic7(temporarysolution);
      }
      else if (heuristicID == 8)
      {
        //applyHeuristic8(temporarysolution);
      }*/
      else
      {
        System.err.println("Heuristic " + heuristicID + "does not exist");
        System.exit(0);
      }
      
      meanTemporary+= objectiveFunction/nbExecution;
      
      //this.heuristicCallTimeRecord[heuristicID] += (int)(System.currentTimeMillis() - startTime);
    }
   
     if (this.heuristicMeanRecord[heuristicID-1][subHeuristicID-1]==0.0)
    	 this.heuristicMeanRecord[heuristicID-1][subHeuristicID-1]= meanTemporary; 
     else
    	 this.heuristicMeanRecord[heuristicID-1][subHeuristicID-1]= (this.heuristicMeanRecord[heuristicID-1][subHeuristicID-1]+meanTemporary)/2 ;
     
     return this.heuristicMeanRecord[heuristicID-1][subHeuristicID-1];
  
  }
  

  
  public Solution deepCopyTheSolution(Solution solutionToCopy)
  {
    Solution newsolution = new Solution();
    for (int x = 0; x < solutionToCopy.variables.length; x++) {
      newsolution.variables[x] = solutionToCopy.variables[x].clone();
    }
    return newsolution;
  }
  
  public void copySolution(int source, int destination)
  {
    Solution tempvariables = deepCopyTheSolution(this.solutionMemory[source]);
    this.solutionMemory[destination] = tempvariables;
  }
  

  public double evaluateObjectiveFunction(Solution solution)
  {
    return solution.numberOfBrokenClauses();
  }
  
  public double getFunctionValue(int solutionIndex)
  {
    return evaluateObjectiveFunction(this.solutionMemory[solutionIndex]);
  }
    
  
  
  private void readInInstance(BufferedReader buffread)
  {
    try
    {
      String readline = "";
      boolean carryon = true;
      while (carryon)
      {
        readline = buffread.readLine();
        if (readline.startsWith("p")) {
          carryon = false;
        }
      }
      this.numberOfVariables = Integer.parseInt(readline.split(" ")[2]);
      if (readline.split(" ").length == 5)
      {
        this.numberOfClauses = Integer.parseInt(readline.split(" ")[4]);
      }
      else if (readline.split(" ").length == 4)
      {
        this.numberOfClauses = Integer.parseInt(readline.split(" ")[3]);
      }
      else
      {
        System.out.println("file format incorrect");
        System.exit(0);
      }
      this.clauses = new Clause[this.numberOfClauses];
      for (int clause = 0; clause < this.numberOfClauses; clause++)
      {
        readline = buffread.readLine();
        readline = readline.trim();
        String[] variables = readline.split(" ");
        Clause C = new Clause(variables.length - 1, clause);
        for (int v = 0; v < variables.length - 1; v++) {
          C.addVariable(Integer.parseInt(variables[v]));
        }
        this.clauses[clause] = C;
      }
    }
    catch (IOException b)
    {
      System.err.println(b.getMessage());
      System.exit(0);
    }
  }
  
  public void loadInstance(String filename)
  {
    try
    {
      FileReader read = new FileReader(filename);
      BufferedReader buffread = new BufferedReader(read);
      readInInstance(buffread);
    }
    catch (FileNotFoundException a)
    {
      try
      {
        InputStream fis = getClass().getClassLoader().getResourceAsStream(filename);
        BufferedReader buffread = new BufferedReader(new InputStreamReader(fis));
        readInInstance(buffread);
      }
      catch (NullPointerException n)
      {
        System.err.println("cannot find file " + filename);
        System.exit(-1);
      }
    }
  }
  
  
  
  public void setMemorySize(int size)
  {
    Solution[] newSolutionMemory = new Solution[size];
    if (this.solutionMemory != null) {
      for (int x = 0; x < this.solutionMemory.length; x++) {
        if (x < size) {
          newSolutionMemory[x] = this.solutionMemory[x];
        }
      }
    }
    this.solutionMemory = newSolutionMemory;
  }
  
  public String solutionToString (int Solution)
  {
	  int heuristicID=Solution/10;
	  int subHeuristicID=Solution%10;
	  String temp = null;
	   int MaxInvers;
	int MaxEssai;
	double proba;
	if (heuristicID == 1)
      {
    	 MaxInvers=this.Gsat_Parametres[subHeuristicID-1].maxInvers;
    	 MaxEssai=this.Gsat_Parametres[subHeuristicID-1].maxEssai;
    	temp="Gsat avec les paramétres suivant : MaxEssai = "+MaxInvers+",MaxInvers = "+MaxEssai;	 
      }
      else if (heuristicID == 2)
      {
    	  MaxInvers=this.Hsat_Parametres[subHeuristicID-1].maxInvers;
     	 MaxEssai=this.Hsat_Parametres[subHeuristicID-1].maxEssai;
     	temp="Hsat avec les paramétres suivant : MaxEssai = "+MaxInvers+",MaxInvers = "+MaxEssai;	 
      }
      else if (heuristicID == 3)
      {
    	  MaxInvers=this.Walk_Parametres[subHeuristicID-1].maxInvers;
      	 MaxEssai=this.Walk_Parametres[subHeuristicID-1].maxEssai;
      	 proba=this.Walk_Parametres[subHeuristicID-1].proba;
      	temp="Walksat avec les paramétres suivant : MaxEssai = "+MaxInvers+",MaxInvers = "+MaxEssai+",la probabilité = "+proba;	 
      }
      else if (heuristicID == 4)
      {
    	  MaxInvers=this.Novelty_Parametres[subHeuristicID-1].maxInvers;
       	 MaxEssai=this.Novelty_Parametres[subHeuristicID-1].maxEssai;
       	 proba=this.Novelty_Parametres[subHeuristicID-1].proba;
       	temp="Novelty avec les paramétres suivant : MaxEssai = "+MaxInvers+",MaxInvers = "+MaxEssai+",la probabilité = "+proba;	 
      }
      else if (heuristicID == 5)
      {
    	  MaxInvers=this.RLRAndomFlip_Parametres[subHeuristicID-1].maxInvers;
      	 MaxEssai=this.RLRAndomFlip_Parametres[subHeuristicID-1].maxEssai;
      	temp="La premiére Recherche locale avec les paramétres suivant : MaxEssai = "+MaxInvers+",MaxInvers = "+MaxEssai;	 
      }
      else if (heuristicID == 6)
      {
    	  MaxInvers=this.RLRAndomFlipBc_Parametres[subHeuristicID-1].maxInvers;
       	 MaxEssai=this.RLRAndomFlipBc_Parametres[subHeuristicID-1].maxEssai;
       	temp="La deuxieme Recherche locale avec les paramétres suivant : MaxEssai = "+MaxInvers+",MaxInvers = "+MaxEssai;	 
      }
	
	return temp;
	  
  }
  /*public String solutionToString(Solution s)
  {
    String solutionstring = "";
    for (int y = 0; y < this.numberOfVariables; y++) {
      solutionstring = solutionstring + s.variables[y].number + ":" + s.variables[y].state + " ";
    }
    return solutionstring;
  }
  
  public String solutionToString(int solutionIndex)
  {
    return solutionToString(this.solutionMemory[solutionIndex]);
  }
  
  public String toString()
  {
    return "SAT";
  }
  
  public boolean compareSolutions(int solutionIndex1, int solutionIndex2)
  {
    Solution s1 = this.solutionMemory[solutionIndex1];
    Solution s2 = this.solutionMemory[solutionIndex2];
    for (int i = 0; i < this.numberOfVariables; i++) {
      if (s1.variables[i].state != s2.variables[i].state) {
        return false;
      }
    }
    return true;
  }
  */
 public class Variable
  {
    public boolean state;
    public int number;
    public int age;
    
    public Variable(int n)
    {
      this.number = n;
      this.state = false;
      this.age = 0;
    }
    
    public void permanentflip()
    {
      if (this.state) {
        this.state = false;
      } else {
        this.state = true;
      }
      this.age = 0;
    }
    
    public void testflip()
    {
      if (this.state) {
        this.state = false;
      } else {
        this.state = true;
      }
    }
    
    public void incrementAge()
    {
      this.age += 1;
    }
    
    public Variable clone()
    {
      Variable v = new Variable(this.number);
      v.state = this.state;
      v.age = this.age;
      return v;
    }
  }
  
 public class Clause
  {
    public int number;
    public int[] variablenumbers;
    public boolean[] variablesigns;
    public int clausefill;
    
    public Clause(int numberofvariables, int num)
    {
      if (numberofvariables == 0)
      {
        System.out.println("zero variables in this clause");
        System.exit(0);
      }
      this.number = num;
      this.variablenumbers = new int[numberofvariables];
      this.variablesigns = new boolean[numberofvariables];
      this.clausefill = 0;
    }
    
    public void addVariable(int n)
    {
      if (n > 0)
      {
        this.variablenumbers[this.clausefill] = (n - 1);
        this.variablesigns[this.clausefill] = true;
      }
      else
      {
        this.variablenumbers[this.clausefill] = (n * -1 - 1);
        this.variablesigns[this.clausefill] = false;
      }
      this.clausefill += 1;
    }
    
    public int numberOfVariables()
    {
      return this.variablenumbers.length;
    }
    
    public boolean getVariableSign(int index)
    {
      return this.variablesigns[index];
    }
    
    public int getVariableNumber(int index)
    {
      return this.variablenumbers[index];
    }
    
    public String clauseToString(SAT.Variable[] variables)
    {
      String s = "";
      s = s + "( ";
      for (int x = 0; x < this.variablenumbers.length; x++)
      {
        if (!this.variablesigns[x] ) {
          s = s + "-";
        }
        s = s + this.variablenumbers[x] + ":" + (this.variablesigns[x] == variables[this.variablenumbers[x]].state) + " ";
      }
      s = s + ")";
      return s;
    }
    
    public void printclause(SAT.Variable[] variables)
    {
      System.out.print("( ");
      for (int x = 0; x < this.variablenumbers.length; x++) {
        System.out.print((this.variablesigns[x] == variables[this.variablenumbers[x]].state) + " ");
      }
      System.out.println(")");
    }
    
    public boolean evaluate(SAT.Variable[] variables)
    {
      for (int x = 0; x < this.variablenumbers.length; x++) {
        if (this.variablesigns[x] == variables[this.variablenumbers[x]].state) {
          return true;
        }
      }
      return false;
    }
    
    public boolean equals(Clause c)
    {
      if (c.number == this.number) {
        return true;
      }
      return false;
    }
  }
 
 
 
 
 
  public class Solution
  {
    public SAT.Variable[] variables;
    
    public Solution()
    {
      this.variables = new SAT.Variable[SAT.this.numberOfVariables];
      for (int x = 0; x < SAT.this.numberOfVariables; x++) {
        this.variables[x] = new SAT.Variable( x);
      }
    }
    
    public int numberOfBrokenClauses()
    {
      int numberbroken = 0;
      for (int x = 0; x < SAT.this.numberOfClauses; x++) {
        if (!SAT.this.clauses[x].evaluate(this.variables)) {
          numberbroken++;
        }
      }
      return numberbroken;
    }
    
    public void incrementAge()
    {
      for (int x = 0; x < SAT.this.numberOfVariables; x++) {
        this.variables[x].incrementAge();
      }
    }
    
    public int testFlipForBrokenClauses(int variableToFlip)
    {
      this.variables[variableToFlip].testflip();
      int numberbroken = 0;
      for (int x = 0; x < SAT.this.numberOfClauses; x++) {
        if (!SAT.this.clauses[x].evaluate(this.variables)) {
          numberbroken++;
        }
      }
      this.variables[variableToFlip].testflip();
      return numberbroken;
    }
    
    public int testFlipForNegGain(int variableToFlip)
    {
      ArrayList<SAT.Clause> satisfiedClauses = new ArrayList<Clause>(SAT.this.numberOfClauses);
      for (int x = 0; x < SAT.this.numberOfClauses; x++) {
        if (SAT.this.clauses[x].evaluate(this.variables)) {
          satisfiedClauses.add(SAT.this.clauses[x]);
        }
      }
      this.variables[variableToFlip].testflip();
      ArrayList<SAT.Clause> newbrokenClauses = new ArrayList<Clause>(50);
      for (int x = 0; x < SAT.this.numberOfClauses; x++) {
        if (!SAT.this.clauses[x].evaluate(this.variables)) {
          newbrokenClauses.add(SAT.this.clauses[x]);
        }
      }
      int numberNotNowSatisfied = 0;
      for (int x = 0; x < satisfiedClauses.size(); x++) {
        if (newbrokenClauses.contains(satisfiedClauses.get(x))) {
          numberNotNowSatisfied++;
        }
      }
      this.variables[variableToFlip].testflip();
      return numberNotNowSatisfied;
    }
  }
}
