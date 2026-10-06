# AGENTS.md

Guia para agentes de IA (e humanos) que trabalham neste repositório.

## Visão geral

Sistema web de **controle financeiro pessoal** (receitas, despesas, cartões de crédito, faturas, parcelas e despesas feitas em nome de terceiros). Aplicação monolítica server-side renderizada.

- **Stack:** Java 17, Spring Boot 4.1 (Web MVC, Data JPA, Validation, DevTools), Thymeleaf, PostgreSQL, Apache POI (dependência presente, ainda sem uso).
- **Front-end:** templates Thymeleaf + Tailwind via CDN (`cdn.tailwindcss.com`, `darkMode: 'class'`), fonte Inter (Google Fonts). Não há build de front-end.
- **Deploy:** Docker multi-stage (`Dockerfile`) no Render, porta exposta `10000`.
- **Idioma:** código, mensagens, commits e UI em **português (pt-BR)**.

## Comandos

```bash
./mvnw spring-boot:run                       # roda local (Windows: mvnw.cmd)
./mvnw clean package                         # build + testes
./mvnw clean package -DskipTests             # build usado no Dockerfile
./mvnw test                                  # testes
```

O único teste (`FinanceiroApplicationTests.contextLoads`) sobe o contexto completo e **precisa de um PostgreSQL acessível**.

## Configuração / profiles

- `application.properties`: config comum (JPA `ddl-auto=update`, `show-sql=true`, cache do Thymeleaf desligado).
- `application-dev.properties`: Postgres local em `localhost:5432/financeiro`; senha via `DBPassword` (variável de ambiente ou arquivo `.env` na raiz, não versionado) (URL/usuário têm padrão, sobrescrevíveis por `DBUrl`/`DBUsername`).
- `application-prod.properties`: lê `DBUrl`, `DBUsername`, `DBPassword` de variáveis de ambiente.
- Nenhum profile é ativado por padrão — use `SPRING_PROFILES_ACTIVE=dev` ou `prod`.
- **Não há migrations** (Flyway/Liquibase): o schema é gerado pelo Hibernate (`ddl-auto=update`). Renomear/remover campos de entidade não apaga colunas antigas no banco — trate mudanças de schema com cuidado.
- Colunas `@Enumerated(STRING)` ganham um `CHECK` com os valores do enum que o `update` nunca atualiza. `config/EnumConstraintsConfig` recria esses CHECKs na subida; ao criar outra coluna enum, registre-a lá.
- Não adicione novas credenciais em arquivos versionados; use variáveis de ambiente.

## Estrutura

```
src/main/java/com/gabriel/financeiro/
  config/       autenticação (interceptor, sessão, BCrypt, advice da navbar)
  controller/   @Controller MVC — retornam nomes de templates ou "redirect:/..."
  service/      regras de negócio
  repository/   interfaces JpaRepository (JPQL com text blocks """ ... """)
  entities/     entidades JPA
  enums/        StatusFatura, StatusParcela
src/main/resources/
  templates/            páginas Thymeleaf (uma por tela)
  templates/fragments/  navbar.html (incluída via th:replace="~{fragments/navbar :: navbar}")
  static/js/theme.js    lógica de tema claro/escuro (os templates hoje repetem esse script inline)
```

## Domínio

| Entidade | Papel |
|---|---|
| `Categoria` | `tipo` é String: `"DESPESA"` ou `"RECEITA"` |
| `Receita` | entrada de dinheiro |
| `Despesa` | gasto; tem `Categoria`, `FormaPagamento`, opcionalmente `CartaoCredito`, `Pessoa` e `CicloFinanceiro` |
| `Pessoa` | terceiro. **Despesa com `pessoa == null` é do próprio usuário**; com pessoa, é de terceiro e pode ser marcada `devolvido` |
| `FormaPagamento` | seeds criados no `@PostConstruct` de `FormaPagamentoService`: `CARTAO_CREDITO`, `CARTAO_VISTA`, `PIX`, `DINHEIRO`, `DEBITO` |
| `CartaoCredito` | tem `diaFechamento` (e vencimento) usados para calcular faturas |
| `FaturaCartao` | uma por cartão + `mesReferencia`/`anoReferencia` (unique); `valorTotal` e `statusFatura` são recalculados a partir das parcelas por `FaturaCartaoService.sincronizar` |
| `Parcela` | gerada para despesas no cartão; ligada a uma `Despesa` e a uma `FaturaCartao` |
| `CicloFinanceiro` / `ConfiguracaoFinanceira` | ciclo mensal baseado no `diaFechamento` global |
| `Usuario` | nome, sobrenome, CPF (único, salvo só com 11 dígitos, validado pelos dígitos verificadores) e `dataNascimento` (`@DateTimeFormat` ISO para o `<input type="date">`); `senha` guarda **somente o hash BCrypt** (nulo = primeiro acesso pendente) |

Regras importantes (ver `DespesaService`, `FaturaCartaoService`, `CicloFinanceiroService`, `ParcelaService`):

- **Inserir despesa no cartão** gera `qtdParcelas` parcelas; o valor é dividido com `RoundingMode.DOWN` e a última parcela recebe o resto. A 1ª parcela vai para a fatura de `calcularReferencia(cartao, dataCompra)` e a parcela i para essa referência `+ (i-1)` meses (somar meses ao `YearMonth`, nunca à data).
- **Despesa recorrente** (`recorrente` é `@Transient`, só do formulário): gera `totalOcorrencias` despesas (2–60) de uma vez, uma por mês (`dataCompra.plusMonths(i)` sobre a data original), ligadas por `grupoRecorrencia` + `ocorrencia`. Cada ocorrência tem seu próprio ciclo e, no cartão, suas próprias parcelas. "Excluir esta e as próximas" (`/despesa/excluir-recorrencia`) mantém as anteriores. Receita só tem a flag `recorrente`, sem gerar lançamentos.
- `CARTAO_VISTA` força 1 parcela. Formas que não são cartão zeram `cartao` e `qtdParcelas` e não geram parcelas.
- **Compra após o dia de fechamento** do cartão cai na fatura do mês seguinte. Dias de fechamento maiores que o mês são ajustados para o último dia.
- **Excluir despesa** apaga as parcelas antes da despesa e sincroniza as faturas afetadas (fatura sem parcelas é removida). Qualquer operação que altere parcelas deve chamar `faturaService.sincronizar(fatura)`. Alterar fechamento/vencimento do cartão chama `recalcularFaturasDoCartao`.
- Totais "meus" vs. "terceiros" em `ParcelaRepository` filtram por `despesa.pessoa IS NULL / IS NOT NULL` e `devolvido = false`.

## Autenticação

- Login por CPF + senha em `/login` (`LoginController`); sessão HTTP com as chaves de `config/SessaoUsuario` (a sessão é recriada no login).
- `config/AutenticacaoInterceptor` protege todas as rotas, exceto `/login`, `/primeiro-acesso`, `/error` e estáticos (lista em `SegurancaConfig`). Sem sessão: redirect para `/login`; requisição AJAX (`X-Requested-With`) recebe 401 em JSON.
- **Primeiro acesso (uma única vez por CPF):** CPF sem cadastro ou cadastrado sem senha vai para `/primeiro-acesso`, onde o usuário confirma/preenche nome, sobrenome e nascimento e escolhe a senha. Depois que existe hash, o CPF só entra com senha.
- Senha: mínimo 8 caracteres com letras e números, máximo 72 bytes (limite do BCrypt). Hash via bean `PasswordEncoder` (`BCryptPasswordEncoder`, dependência `spring-security-crypto`; o Spring Security completo **não** está no projeto).
- Nunca deixe o formulário bindar `senha` direto na entidade: use `@InitBinder` com `setDisallowedFields("senha")`.
- O nome do usuário logado chega à navbar pelo model attribute `usuarioLogadoNome` (`UsuarioLogadoAdvice`).

## Convenções de código

- Injeção de dependência **por construtor** com campos `private final` (sem `@Autowired`, sem Lombok).
- Entidades: `@Id @GeneratedValue UUID`, construtor vazio + construtor com campos, getters/setters escritos à mão, `equals`/`hashCode` só pelo `id`.
- Valores monetários sempre `BigDecimal`; datas `LocalDate`.
- Erros de negócio: `throw new RuntimeException("mensagem em pt-BR")`. Controllers capturam e usam flash attributes:
  ```java
  redirectAttributes.addFlashAttribute("mensagem", "...");
  redirectAttributes.addFlashAttribute("tipoMensagem", "sucesso" | "erro");
  return "redirect:/rota";
  ```
- Padrão POST-redirect-GET; formulários bindam diretamente na entidade (ex.: `Despesa` com `categoria.id`, `cartao.id`). Services recarregam as associações pelo id antes de salvar.
- Listagens paginadas com `PageRequest.of(page, size, Sort...)` e `@RequestParam(defaultValue = ...)`.
- Seções de código separadas por comentários-banner `// ===== TÍTULO =====`. Nomes de métodos em português (alguns legados em PascalCase, ex. `ListDespesa`; mantenha o existente ao editar).
- Rotas seguem `/{recurso}` (tela/cadastro), `/{recurso}/{plural}` (gerenciar), `/create`, `/update/{id}`, `/delete/{id}` ou `/excluir`.

## Templates / UI

- Toda página nova deve incluir o Tailwind CDN + config `darkMode: 'class'`, o script de tema e `<div th:replace="~{fragments/navbar :: navbar}">`.
- Mensagens flash (`mensagem`/`tipoMensagem`) são exibidas pelo fragmento `<div th:replace="~{fragments/navbar :: flash}">`; inclua-o logo após a navbar.
- Suporte a dark mode é obrigatório: use pares `bg-white dark:bg-slate-900`, `text-slate-900 dark:text-white`, etc.
- Estilo visual: cantos `rounded-xl/2xl`, paleta slate + cor por módulo (receita = emerald, categorias = orange, cartões = amber, etc. — ver navbar).
- Ao adicionar uma tela, adicione o link correspondente em `fragments/navbar.html`.

## Commits

Mensagens em português no estilo Conventional Commits: `feat: ...`, `fix: ...`.
