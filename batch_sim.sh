for c in 500 1000 1500
do
  echo clients=$c
  for t in 300000 180000 60000
  do
    echo origination_rate=$t
    for r in 1 2 3 4 5 6 7 8 9 10
    do
          echo random_seed=$r
          l="c${c}_t${t}_r${r}"
          m=$(( 5 * $c ))
          ./coinspermia_simulator.sh -steps 900000 -numNodes 100 -connectionDensity .2 -numClients $c -transactionOriginationRate $t -mintedCoins $m -randomSeed $r -logfile ${l}.log
    done
  done
done
