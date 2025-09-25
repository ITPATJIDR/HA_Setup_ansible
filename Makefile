playbook:
	ansible-playbook -i hosts.ini ksec-implement.yml --ask-become-pass 

install-jenkins:
	ansible-playbook -i hosts.ini jenkins-install.yml --ask-become-pass 

install-harbor:
	ansible-playbook -i hosts.ini harbor-install.yml --ask-become-pass 

install-vault:
	ansible-playbook -i hosts.ini vault-install.yml --ask-become-pass 

delete-istio:
	ansible-playbook -i hosts.ini delete-istio.yml --ask-become-pass 

install-istio:
	ansible-playbook -i hosts.ini install-istio.yml --ask-become-pass 

create-serviceaccount-jenkins:
	ansible-playbook -i hosts.ini create-serviceaccount-jenkins.yml --ask-become-pass 