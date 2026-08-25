package dev.diego.accommodationbookingservice.controller;

import dev.diego.accommodationbookingservice.security.JwtUtil;
import dev.diego.accommodationbookingservice.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import({JwtUtil.class, SecurityConfig.class})
public abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected JwtUtil jwtUtil;

    @MockitoBean
    protected UserDetailsService userDetailsService;
}
