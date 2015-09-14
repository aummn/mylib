@echo Stopping JBoss Server ...
net stop JBossEAP6
ping 1.1.1.1 -n 1 -w 10000 > nul
@echo JBoss Server Stopped.