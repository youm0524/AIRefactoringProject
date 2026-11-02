package com.careerpick.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CoverLetterNotFoundException extends ResponseStatusException {
    public CoverLetterNotFoundException() { super(HttpStatus.NOT_FOUND, "자기소개서를 찾을 수 없습니다."); }
}
