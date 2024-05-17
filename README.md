# Spring AI - Oracle Database 23ai - RAG


# Prerequisites:
- ojdbc 23.4.0.24.05
- OpenAI api key. https://platform.openai.com/


## Step 1: Setting up
Execute following script to create a user and a table.
setup.sql
```sql
CREATE USER VECTOR_USER IDENTIFIED BY <YOUR_PASSWORD> QUOTA UNLIMITED ON USERS;  
GRANT DB_DEVELOPER_ROLE TO VECTOR_USER;  
GRANT CREATE SESSION TO VECTOR_USER;  
GRANT SELECT ANY TABLE ON SCHEMA VECTOR_USER TO VECTOR_USER;  
ALTER SESSION SET CURRENT_SCHEMA = VECTOR_USER;  
CREATE TABLE VECTOR_STORE (ID VARCHAR2(64) PRIMARY KEY, METADATA VARCHAR(256), CONTENT CLOB, VECTOR_DATA VECTOR(1536, FLOAT64));  
COMMIT;
```

		
pom.xml
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
	<dependency>
		<groupId>org.springframework.ai</groupId>
		<artifactId>spring-ai-openai-spring-boot-starter</artifactId>
	</dependency>    
	<dependency>
		<groupId>com.oracle.database.jdbc</groupId>
		<artifactId>ojdbc11</artifactId>
		<version>23.4.0.24.05</version>
	</dependency>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-jdbc</artifactId>
	</dependency>
	<dependency>
		<groupId>org.springframework.ai</groupId>
		<artifactId>spring-ai-pdf-document-reader</artifactId>
	</dependency>	
</dependencies>
```

application.properties
```properties
spring.datasource.url=jdbc:oracle:thin:@172.17.0.2:1521/FREEPDB1
spring.datasource.username=...
spring.datasource.password=...
spring.ai.openai.api-key=sk-...
spring.ai.openai.chat.options.model=gpt-3.5-turbo
```

## Step 2: Build a DataLoader
This will ingest pdf from resource into vector database on first time.
```java
@Component
public class DataLoader {
	@Autowired JdbcClient jdbcClient;
	@Autowired VectorStore vectorStore;
	    
    @Value("classpath:Cricket_World_Cup.pdf")
    Resource pdfResource;
    
    @PostConstruct
    public void init() {
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM VECTOR_STORE")
                .query(Integer.class)
                .single();
        
        if (count == 0) {
            // Ingesting PDF into Vector Store
            var config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder().withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopPagesToSkipBeforeDelete(0)
                            .build())
                    .withPagesPerDocument(1)
                    .build();

            var pdfReader = new PagePdfDocumentReader(pdfResource, config);
            var textSplitter = new TokenTextSplitter();
            vectorStore.accept(textSplitter.apply(pdfReader.get()));
        }
    }
}

```


## Step 3: Build a VectorStore implementation

Copas from [OracleVectorStore](src/main/java/com/example/springbootoracleaitutorial/OracleVectorStore.java)


Register as bean
```java
@SpringBootApplication
public class Application {
	//...omitted
	@Bean
	public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingClient embeddingClient) {
		return new OracleVectorStore(jdbcTemplate, embeddingClient);
	}
}
```
## Step 4: Create an API
This controller will respond to any chats from user regarding the document.
```java
@RestController
@RequestMapping("/pdf")
public class ControllerPdf {
    @Autowired VectorStore vector;
    @Autowired ChatClient chatClient;

    @GetMapping("/ai/chat")
    public Map<String, String> completion(@RequestParam(value = "message",
            defaultValue = "who won the latest cricket world cup ?") String message) {

        var documents = vector.similaritySearch(message);
        var inlined = documents.stream().map(Document::getContent)
                .collect(Collectors.joining(System.lineSeparator()));
        var similarDocsMessage = new SystemPromptTemplate("Based on the following: {documents}")
                .createMessage(Map.of("documents", inlined));

        var userMessage = new UserMessage(message);
        var prompt = new Prompt(List.of(similarDocsMessage, userMessage));
        return Map.of("generation", chatClient.call(prompt).getResult().getOutput().getContent());
    }
}

```

## Step 5: Test the Application

```sh
$ mvn spring-boot:run
```

let's use curl to test :
```sh
$ curl http://localhost:8080/pdf/ai/chat
{"generation":"The latest ICC Cricket World Cup was held in 2019 in England and Wales. The final match was between England and New Zealand. The match ended in a tie, and it went into a super over. England won the final on the boundaries countback rule."}
```

# References:
- https://docs.oracle.com/en/database/oracle/oracle-database/23/vecse/oracle-ai-vector-search-users-guide.pdf
- https://medium.com/oracledevs/oracle-ai-vector-search-for-java-developers-with-the-oracle-database-23ai-e29321d23fa0


