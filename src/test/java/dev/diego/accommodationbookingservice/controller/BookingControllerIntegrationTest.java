package dev.diego.accommodationbookingservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.diego.accommodationbookingservice.dto.booking.BookingRequestDto;
import dev.diego.accommodationbookingservice.dto.booking.BookingResponseDto;
import dev.diego.accommodationbookingservice.dto.booking.BookingUpdateDto;
import dev.diego.accommodationbookingservice.model.BookingStatus;
import dev.diego.accommodationbookingservice.model.Role;
import dev.diego.accommodationbookingservice.model.User;
import dev.diego.accommodationbookingservice.repository.UserRepository;
import dev.diego.accommodationbookingservice.security.JwtUtil;
import dev.diego.accommodationbookingservice.service.BookingService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private BookingService bookingService;

    private String token;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User customer = new User();
        customer.setEmail("jack.sparrow" + java.util.UUID.randomUUID() + "@blackpearl.com");
        customer.setPassword(passwordEncoder.encode("rum123"));
        customer.setFirstName("Jack");
        customer.setLastName("Sparrow");
        customer.setRole(Role.CUSTOMER);
        User savedUser = userRepository.save(customer);

        token = jwtUtil.generateToken(savedUser.getEmail());
    }

    @Test
    @DisplayName("Should successfully create a new booking")
    void shouldCreateBooking() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(5);

        BookingRequestDto requestDto = new BookingRequestDto(1L, checkIn, checkOut);

        BookingResponseDto responseDto = new BookingResponseDto(
                1L,
                checkIn,
                checkOut,
                1L,
                1L,
                BookingStatus.PENDING
        );

        Mockito.when(bookingService.createBooking(any(User.class), any(BookingRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.accommodationId").value(1));
    }

    @Test
    @DisplayName("Should retrieve current user bookings")
    void shouldGetMyBookings() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(5);

        BookingResponseDto responseDto = new BookingResponseDto(
                1L,
                checkIn,
                checkOut,
                1L,
                1L,
                BookingStatus.CONFIRMED
        );

        Mockito.when(bookingService.getUserBookings(any(Long.class)))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/bookings/my")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("Should retrieve specific booking by ID")
    void shouldGetBookingById() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(5);

        BookingResponseDto responseDto = new BookingResponseDto(
                1L,
                checkIn,
                checkOut,
                1L,
                1L,
                BookingStatus.PENDING
        );

        Mockito.when(bookingService.getBookingById(eq(1L), any(User.class)))
                .thenReturn(responseDto);

        mockMvc.perform(get("/bookings/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Should successfully cancel a booking")
    void shouldCancelBooking() throws Exception {
        Mockito.doNothing().when(bookingService).cancelBooking(eq(1L), any(User.class));

        mockMvc.perform(delete("/bookings/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should retrieve filtered bookings list for admin/manager")
    void shouldGetBookingsFiltered() throws Exception {
        User admin = new User();
        admin.setEmail("admin.manager" + java.util.UUID.randomUUID() + "@blackpearl.com");
        admin.setPassword(passwordEncoder.encode("rum123"));
        admin.setFirstName("Admin");
        admin.setLastName("Manager");
        admin.setRole(Role.ADMIN);
        User savedAdmin = userRepository.save(admin);
        String adminToken = jwtUtil.generateToken(savedAdmin.getEmail());

        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = LocalDate.now().plusDays(5);

        BookingResponseDto responseDto = new BookingResponseDto(
                1L,
                checkIn,
                checkOut,
                1L,
                1L,
                BookingStatus.PENDING
        );

        Mockito.when(bookingService.getBookings(eq(1L), eq(BookingStatus.PENDING)))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/bookings")
                        .param("user_id", "1")
                        .param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("Should successfully update a booking")
    void shouldUpdateBooking() throws Exception {
        LocalDate newCheckIn = LocalDate.now().plusDays(2);
        LocalDate newCheckOut = LocalDate.now().plusDays(6);

        BookingUpdateDto updateDto = new BookingUpdateDto(newCheckIn, newCheckOut);

        BookingResponseDto responseDto = new BookingResponseDto(
                1L,
                newCheckIn,
                newCheckOut,
                1L,
                1L,
                BookingStatus.PENDING
        );

        Mockito.when(bookingService.updateBooking(
                eq(1L), any(User.class), any(BookingUpdateDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/bookings/1")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.checkInDate").value(newCheckIn.toString()))
                .andExpect(jsonPath("$.checkOutDate").value(newCheckOut.toString()));
    }
}
