package com.steelstory.bootstrap.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

final class IdentityRequestLimitFilter extends OncePerRequestFilter {
    private static final int MAX_BYTES = 4096;
    private final SecurityProblemWriter securityProblemWriter;

    IdentityRequestLimitFilter(SecurityProblemWriter securityProblemWriter) {
        this.securityProblemWriter = securityProblemWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getMethod().equals("POST") || !request.getServletPath().startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BYTES) {
            reject(request, response);
            return;
        }

        byte[] body = request.getInputStream().readNBytes(MAX_BYTES + 1);
        if (body.length > MAX_BYTES) {
            reject(request, response);
            return;
        }

        filterChain.doFilter(new BufferedRequest(request, body), response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        securityProblemWriter.write(request, response, HttpStatus.CONTENT_TOO_LARGE,
                "REQUEST_TOO_LARGE", "Identity request body exceeds 4096 bytes.");
    }

    private static final class BufferedRequest extends HttpServletRequestWrapper {
        private final byte[] body;

        BufferedRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);

            return new ServletInputStream() {
                @Override
                public int read() {
                    return input.read();
                }

                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new IllegalStateException("Identity request bodies use synchronous reads.");
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
