# Sistema de inserção de dados via XML em um servidor LDAP

## Requisitos:
* JDK 11
* ApacheDS 2.0

## Configuração do servidor LDAP:
* Na aplicação, utilizamos como servidor LDAP o ApacheDS;
* Você pode baixá-lo em: https://directory.apache.org/apacheds/downloads.html;
* Após a instalação, crie um servidor LDAP na aba "LDAP servers";
* Para criar uma conexão com o servidor, clique com o botão direito no servidor e selecione "Create a Connection".

## Como rodar a aplicação:
* Se a autenticação e a porta do servidor não foram modificadas, basta executar Main.java;
* Caso contrário, no método main altere as linhas:
```
env.put(Context.PROVIDER_URL, "ldap://localhost:10389"); // Para a porta e host
env.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system"); // Para o usuário
env.put(Context.SECURITY_CREDENTIALS, "secret"); // Para a senha
```