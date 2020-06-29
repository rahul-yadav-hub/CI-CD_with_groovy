job("task6_jb1"){
        description("This will pull the developers code from github & filter & save the php and html codes in different directories")
        scm {
                 github('rahul-yadav-hub/DevOps_task2.git' , 'master')
             }
        triggers {
                scm("* * * * *")
                
  	}
        steps {
        shell('''if sudo ls /rahul | grep html
                 then
                 echo "dir for html code is already present"
                 else
                 sudo mkdir /rahul/html
                 fi

                 if sudo ls | grep .html
                 then
                 sudo cp -vrf *.html /rahul/html
                 fi
 
                 if sudo ls /rahul | grep php
                 then
                 echo "dir for php code is already present"
                 else
                 sudo mkdir /rahul/php
                 fi

                 if sudo ls | grep .php
                 then
                 sudo cp -vrf *.php /rahul/php
                 fi''')
      }
}


job("task6_jb2"){
        description("to deploy our applications on k8s")
        
        triggers {
        upstream {
    upstreamProjects("task6_jb1")
    threshold("Fail")
        }
        }
        steps {
        shell('''if sudo ls /rahul | grep html
then
  if sudo kubectl get pvc --kubeconfig /rahul/kubectlconfig | grep html
  then
  echo "pvc for html already created"
  else
  sudo kubectl create -f /rahul/deply/html-pvc.yaml --kubeconfig /rahul/kubectlconfig
  fi
  if sudo kubectl get deploy --kubeconfig /rahul/kubectlconfig | grep html-webserver
  then
    echo "already running"
  else
    sudo kubectl create -f /rahul/deply/html-deply.yaml --kubeconfig /rahul/kubectlconfig
  fi
else
echo "no html code from developer to host"
fi

if sudo ls /rahul | grep php
then
  if sudo kubectl get pvc --kubeconfig /rahul/kubectlconfig | grep php
  then
  echo "pvc for php already created"
  else
  sudo kubectl create -f /rahul/deply/php-pvc.yaml --kubeconfig /rahul/kubectlconfig
  fi
  if sudo kubectl get deploy --kubeconfig /rahul/kubectlconfig | grep php-webserver
  then
    echo "already running"
  else
    sudo kubectl create -f /rahul/deply/php-deply.yaml --kubeconfig /rahul/kubectlconfig
  fi
else
echo "no php code from developer to host"
fi


 sleep 60
htmlpod=$(sudo kubectl get pod -l app=html-webserver -o jsonpath="{.items[0].metadata.name}" --kubeconfig /rahul/kubectlconfig)
sudo kubectl cp /rahul/html/*   $htmlpod:/usr/local/apache2/htdocs --kubeconfig /rahul/kubectlconfig
phppod=$(sudo kubectl get pod -l app=php-webserver -o jsonpath="{.items[0].metadata.name}" --kubeconfig /rahul/kubectlconfig)
sudo kubectl cp /rahul/php/* $phppod:/var/www/html --kubeconfig /rahul/kubectlconfig
''')
      }
}


job("task6_jb3"){
        description("Testing applications")
        
        triggers {
                upstream {
    upstreamProjects("task6_jb2")
    threshold("Fail")
   } 
        }
        steps {
        shell('''status=$(curl -o /dev/null -sw "%{http_code}" http://192.168.99.100:30001/web.html)
 if [[$status == 200 ]]
then
echo "Running Good"
else
flag-html=1
fi

status=$(curl -o /dev/null -sw "%{http_code}" http://192.168.99.100:30002/web1.php)
 if [[$status == 200 ]]
then
echo "Running Good"
else
flag-php=1
fi''')
      }
}

job("task6_jb4"){
        description("email notification")
        steps {
        shell('''
if [ $flag-html -eq 1 ] || [ flag-php -eq 1 ]
then
sudo python3 /rahul/mail.py
fi''')
      }
}

buildPipelineView('DevOps-task') {
    filterBuildQueue()
    filterExecutors()
    title('CI/CD Pipeline')
    displayedBuilds(3)
    selectedJob('task6_jb1')
    alwaysAllowManualTrigger()
    showPipelineParameters()
    refreshFrequency(5)
}
