# Controle de Gastos

App Android (Kotlin + Jetpack Compose) para controle de gastos do casal, com
lançamento de despesas — incluindo parcelamento no cartão de crédito — e
sincronização em tempo real com uma planilha compartilhada no Google Sheets.

## Como funciona

- Cada pessoa instala o app no próprio celular e faz login com a própria conta Google.
- Os gastos ficam salvos localmente no celular (offline-first) e são sincronizados
  automaticamente com a planilha compartilhada assim que há conexão com a internet.
- A planilha do Google Sheets é a fonte única de dados: os dois celulares leem e
  escrevem na mesma planilha, então o que uma pessoa lança aparece para a outra.
- Compras parceladas no crédito geram uma linha por parcela, já calculando em qual
  fatura (mês de competência) cada parcela cai, com base no dia de fechamento do cartão.
- Dados antigos (anteriores ao uso do app) podem ser digitados manualmente direto na
  planilha — o app não precisa nem sabe lidar com eles, e eles não interferem no app
  porque a listagem do app mostra só o mês atual e o anterior.

## Estrutura do projeto

```
app/src/main/java/com/flpzarato/controlegastos/
├── data/
│   ├── local/        Room: entidades, DAOs e banco de dados local (cache offline)
│   ├── remote/       Cliente Retrofit da Google Sheets API + mapeamento linha↔entidade
│   ├── repository/   Camada que o resto do app usa (esconde Room/Sheets)
│   └── prefs/         DataStore: conta logada, ID da planilha, última sincronização
├── domain/           Regras de negócio puras (cálculo de parcelas e competência)
├── sync/             WorkManager: agenda e executa a sincronização em segundo plano
├── di/               Módulos do Hilt (injeção de dependência)
└── ui/               Telas em Jetpack Compose (login, lançamentos, categorias, cartões)
```

## Configuração necessária (feita uma única vez)

O código já está pronto, mas três coisas dependem da sua própria conta Google e
não podem ser feitas por mim:

### 1. Criar a planilha compartilhada no Google Sheets

Crie uma planilha nova e adicione 4 abas com exatamente estes nomes e cabeçalhos
na primeira linha (os dados começam na linha 2):

| Aba | Colunas (linha 1) |
|---|---|
| `Lançamentos` | ID, DataCompra, Competencia, Descricao, CategoriaId, ValorParcela, ValorTotalOriginal, Parcela, FormaPagamento, CartaoId, QuemLancou, Deletado |
| `Categorias` | ID, Nome, Deletado |
| `Cartões` | ID, Apelido, TitularEmail, Banco, DiaFechamento, DiaVencimento, Deletado |
| `Usuários` | Email, Nome |

Compartilhe essa planilha (botão "Compartilhar") com a conta Google da sua esposa,
dando permissão de **Editor**.

Pegue o **ID da planilha**: é o trecho na URL entre `/d/` e `/edit`, por exemplo
`https://docs.google.com/spreadsheets/d/`**`1A2B3C...xyz`**`/edit` — você vai colar
esse ID (ou a URL inteira) na tela de login do app na primeira vez que abrir.

### 2. Configurar o Google Cloud (API do Sheets + login com Google)

1. Acesse https://console.cloud.google.com/ e crie um projeto nome (ex: "Controle de Gastos").
2. Em **APIs e serviços > Biblioteca**, ative a **Google Sheets API**.
3. Em **APIs e serviços > Tela de consentimento OAuth**: tipo **Externo**, adicione
   seu e-mail e o da sua esposa como **usuários de teste** (dispensa o processo de
   verificação do Google, já que o app nunca sai do círculo de vocês dois).
4. Em **APIs e serviços > Credenciais > Criar credenciais > ID do cliente OAuth**:
   - Crie um client do tipo **Aplicativo da Web** (sim, "Web", mesmo sendo um app
     Android — é esse client ID que o Credential Manager usa para o login). Copie o
     Client ID gerado.
   - Crie também um client do tipo **Android**, informando o `applicationId`
     (`com.flpzarato.controlegastos`) e o **SHA-1** do seu certificado de assinatura
     (para o debug, rode `./gradlew signingReport` e copie o SHA-1 da variante *debug*).
5. Copie `secrets.xml.example` (raiz do projeto) para
   `app/src/main/res/values/secrets.xml` e cole o Client ID do tipo **Web** em
   `default_web_client_id`. Esse arquivo não é versionado no Git.

### 3. (Opcional) Build automático via GitHub Actions

O workflow em `.github/workflows/build.yml` já compila e sobe o APK como artefato
a cada push. Para ele também preencher o `default_web_client_id` corretamente,
adicione um secret do repositório chamado `DEFAULT_WEB_CLIENT_ID` com o mesmo
Client ID do passo anterior (Settings > Secrets and variables > Actions).

## Rodando o projeto

Abra a pasta no Android Studio (ele baixa o Gradle e as dependências automaticamente)
e rode no emulador ou num celular físico. Pelo terminal:

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

O APK fica em `app/build/outputs/apk/debug/app-debug.apk` — copie para o celular e
instale manualmente (ative "Instalar de fontes desconhecidas" no Android).

## Fora de escopo (por enquanto)

Por decisão consciente, a primeira versão não inclui: foto de comprovante, gastos
recorrentes automáticos, orçamento/alertas por categoria, nem publicação na Play
Store — tudo isso foi discutido e deixado de fora do MVP para manter o app simples.

## Licença

MIT — veja [LICENSE](LICENSE).
