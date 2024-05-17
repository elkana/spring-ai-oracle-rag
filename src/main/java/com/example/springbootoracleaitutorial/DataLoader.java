package com.example.springbootoracleaitutorial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class DataLoader {
    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    
    final JdbcClient jdbcClient;
    final VectorStore vectorStore;
    
    @Value("classpath:Cricket_World_Cup.pdf")
    private Resource pdfResource;

    DataLoader(JdbcClient jdbcClient, VectorStore vectorStore) {
        this.jdbcClient = jdbcClient;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void init() {
        Integer count = jdbcClient.sql("select count(*) from " + OracleVectorStore.TABLE_NAME)
                .query(Integer.class).single();

        log.info("Total Rows: {}", count);
        if (count == 0) {
            log.info("Ingesting PDF into Vector Store");
            var config = PdfDocumentReaderConfig.builder()
                    .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                            .withNumberOfBottomTextLinesToDelete(0)
                            .withNumberOfTopPagesToSkipBeforeDelete(0).build())
                    .withPagesPerDocument(1).build();

            var pdfReader = new PagePdfDocumentReader(pdfResource, config);
            var textSplitter = new TokenTextSplitter();
            vectorStore.accept(textSplitter.apply(pdfReader.get()));

            log.info("Application is ready");
        }
    }
}
