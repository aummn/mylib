@echo Starting JBoss Server ...
ping 1.1.1.1 -n 1 -w 10000 > nul
net start JBossEAP6
@echo JBoss Server started.