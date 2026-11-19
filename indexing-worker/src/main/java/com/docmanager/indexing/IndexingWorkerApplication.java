package com.docmanager.indexing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IndexingWorkerApplication {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(IndexingWorkerApplication.class, args);
        Thread.currentThread().join();
    }
}
