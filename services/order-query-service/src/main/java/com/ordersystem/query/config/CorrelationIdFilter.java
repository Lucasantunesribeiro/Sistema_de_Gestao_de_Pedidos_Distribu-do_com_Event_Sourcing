package com.ordersystem.query.config;

ponent

ic class

final String CORRELATION_ID_HEADER=

tion,

st httpRequest=(HttpServletRequest)request;httpResponse=(HttpServletResponse)response

Id=httpRequest.getHeader(CORRELATION_ID_HEADE==null||correlationId.trim().isEmpty()){

ATION_ID_MD ader(CORRELATION_ID_HEADER,correla

er(request,response);

y{ORRELATION_ID_MDC_KEY);

}