package com.ordersystem.query.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

  
  ponent

  ic class 
  
       final String CORRELATION_ID_HEADER = 

    
    

    tion,
      
      st httpRequest = (HttpServletRequest) request;
        httpResponse = (HttpServletResponse) response
      

      Id = httpRequest.getHeader(CORRELATION_ID_HEADE
      == null || correlationId.trim().isEmpty()) {

      

    ATION_ID_MD
      ader(CORRELATION_ID_HEADER, correla
    
    er(request, response);

    y {
      ORRELATION_ID_MDC_KEY);
    
  
}