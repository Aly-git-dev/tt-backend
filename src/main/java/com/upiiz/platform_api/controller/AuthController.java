package com.upiiz.platform_api.controller;

import com.upiiz.platform_api.auth.dto.LoginRequest;
import com.upiiz.platform_api.auth.dto.RegisterRequest;
import com.upiiz.platform_api.auth.dto.TokensResponse;
import com.upiiz.platform_api.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@CrossOrigin(
        origins = "http://148.204.142.20:3030",
        allowCredentials = "true"
)
@RestController
@RequestMapping("/upiiz/public/v1/auth")
@Tag(name = "Autenticación", description = "Endpoints públicos para registro, login y manejo de tokens")
public class AuthController {

    private final AuthService svc;

    public AuthController(AuthService svc) {
        this.svc = svc;
    }

    // ---------- REGISTRO LOCAL (PROFESOR/ADMIN) ----------
    @PostMapping("/registro")
    @Operation(
            summary = "Registro de usuario local",
            description = "Registra un nuevo usuario (profesor/admin) en la plataforma y envía correo de confirmación."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuario registrado correctamente, pendiente de confirmación de correo",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Registro exitoso",
                                    value = "{\n" +
                                            "  \"estado\": 1,\n" +
                                            "  \"mensaje\": \"Registro exitoso. Revisa tu correo para confirmar la cuenta.\",\n" +
                                            "  \"usuarioId\": \"f6c6c4bb-5320-4f5f-9e21-8d2e1df3a012\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de registro inválidos (campos faltantes, formato incorrecto, etc.)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Error de validación",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"El correo institucional es obligatorio\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto, por ejemplo si el correo ya está registrado o el usuario ya fue confirmado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Correo ya registrado",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Ya existe un usuario registrado con ese correo institucional\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al registrar al usuario o enviar el correo de confirmación",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Error interno",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Error interno\",\n" +
                                            "  \"error\": \"Detalle técnico del error\"\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<?> registro(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos necesarios para registrar al usuario",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo de registro",
                                    value = "{\n" +
                                            "  \"emailInst\": \"user@upiiz.ipn.mx\",\n" +
                                            "  \"password\": \"Password123!\",\n" +
                                            "  \"fullName\": \"Nombre Apellido\",\n" +
                                            "  \"role\": \"PROFESOR\"\n" +
                                            "}"
                            )
                    )
            )
            @RequestBody RegisterRequest r,
            @Parameter(
                    description = "URL base de la app cliente para construir enlaces de confirmación. " +
                            "Si no se envía, se usa http://localhost:8080",
                    required = false,
                    example = "https://platform-upiiz.dev"
            )
            @RequestHeader(value = "X-App-BaseUrl", required = false) String baseUrl
    ) {
        try {
            String appBase = (baseUrl == null || baseUrl.isBlank()) ? "http://localhost:8080" : baseUrl;
            var res = svc.register(r, appBase);
            return ResponseEntity.status(201).body(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("estado", 0, "mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("estado", 0, "mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("estado", 0, "mensaje", "Error interno", "error", e.getMessage()));
        }
    }

    // ---------- CONFIRMAR CORREO ----------
    @GetMapping("/confirm")
    @Operation(
            summary = "Confirmar correo electrónico",
            description = "Confirma la cuenta del usuario utilizando el token enviado por correo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Correo confirmado correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Confirmación exitosa",
                                    value = "{\n" +
                                            "  \"estado\": 1,\n" +
                                            "  \"mensaje\": \"Correo confirmado correctamente\",\n" +
                                            "  \"usuarioId\": \"f6c6c4bb-5320-4f5f-9e21-8d2e1df3a012\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Token inválido o con formato incorrecto",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Token inválido",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Token de confirmación inválido\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto, por ejemplo si el correo ya estaba confirmado previamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Correo ya confirmado",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"La cuenta ya fue confirmada previamente\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al confirmar el correo",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Error interno",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Error interno\",\n" +
                                            "  \"error\": \"Detalle técnico del error\"\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<?> confirm(
            @Parameter(
                    description = "Token de confirmación enviado al correo del usuario",
                    required = true,
                    example = "f6c6c4bb-5320-4f5f-9e21-8d2e1df3a012"
            )
            @RequestParam("token") UUID token
    ) {
        try {
            return ResponseEntity.ok(svc.confirmEmail(token));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("estado", 0, "mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("estado", 0, "mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("estado", 0, "mensaje", "Error interno", "error", e.getMessage()));
        }
    }

    // ---------- APROBAR USUARIO (ADMIN) ----------
    @PatchMapping("/approve/{userId}")
    @Operation(
            summary = "Aprobar usuario",
            description = "Marca a un usuario como aprobado por un administrador para que pueda acceder a la plataforma."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario aprobado correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Aprobación exitosa",
                                    value = "{\n" +
                                            "  \"estado\": 1,\n" +
                                            "  \"mensaje\": \"Usuario aprobado correctamente\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario no encontrado con el ID proporcionado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Usuario no encontrado",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Usuario no encontrado\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al intentar aprobar al usuario",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Error interno",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Error interno\",\n" +
                                            "  \"error\": \"Detalle técnico del error\"\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<?> approve(
            @Parameter(
                    description = "Identificador único del usuario a aprobar",
                    required = true,
                    example = "f6c6c4bb-5320-4f5f-9e21-8d2e1df3a012"
            )
            @PathVariable UUID userId
    ) {
        try {
            return ResponseEntity.ok(svc.approveUser(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("estado", 0, "mensaje", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("estado", 0, "mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("estado", 0, "mensaje", "Error interno", "error", e.getMessage()));
        }
    }

    // ---------- LOGIN ----------
    @PostMapping("/login")
    @Operation(
            summary = "Login con credenciales locales",
            description = "Autentica al usuario con su correo y contraseña, devolviendo tokens de acceso y refresh."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Usuario autenticado correctamente, se devuelven tokens JWT",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Login exitoso",
                                    value = "{\n" +
                                            "  \"estado\": 1,\n" +
                                            "  \"mensaje\": \"Usuario autenticado\",\n" +
                                            "  \"tokens\": {\n" +
                                            "    \"accessToken\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...\",\n" +
                                            "    \"refreshToken\": \"c32d8a12-3cfc-4f5d-a4cf-3a3b3d5a9a10\",\n" +
                                            "    \"expiresIn\": 3600\n" +
                                            "  }\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales inválidas o usuario no autorizado para acceder",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Credenciales inválidas",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Correo o contraseña incorrectos\"\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno al procesar el login",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Error interno",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Error interno\",\n" +
                                            "  \"error\": \"Detalle técnico del error\"\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<?> login(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Credenciales de acceso del usuario",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo de login",
                                    value = "{\n" +
                                            "  \"username\": \"user@upiiz.ipn.mx\",\n" +
                                            "  \"password\": \"Password123!\"\n" +
                                            "}"
                            )
                    )
            )
            @RequestBody LoginRequest req
    ) {
        try {
            TokensResponse tokens = svc.login(req);
            return ResponseEntity.ok(Map.of("estado", 1, "mensaje", "Usuario autenticado", "tokens", tokens));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("estado", 0, "mensaje", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("estado", 0, "mensaje", "Error interno", "error", e.getMessage()));
        }
    }

    // ---------- REFRESH ----------
    @PostMapping("/refresh")
    @Operation(
            summary = "Renovar token de acceso",
            description = "Recibe un refresh token válido y devuelve un nuevo par de tokens."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token renovado correctamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Refresh exitoso",
                                    value = "{\n" +
                                            "  \"estado\": 1,\n" +
                                            "  \"mensaje\": \"Token renovado\",\n" +
                                            "  \"tokens\": {\n" +
                                            "    \"accessToken\": \"nuevoAccessToken...\",\n" +
                                            "    \"refreshToken\": \"nuevoRefreshToken...\",\n" +
                                            "    \"expiresIn\": 3600\n" +
                                            "  }\n" +
                                            "}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token inválido o expirado",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Refresh token inválido",
                                    value = "{\n" +
                                            "  \"estado\": 0,\n" +
                                            "  \"mensaje\": \"Token inválido/expirado\"\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<?> refresh(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Objeto con el refresh token actual",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TokensResponse.class),
                            examples = @ExampleObject(
                                    name = "Ejemplo de refresh",
                                    value = "{\n" +
                                            "  \"accessToken\": \"accessTokenActual...\",\n" +
                                            "  \"refreshToken\": \"refreshTokenActual...\",\n" +
                                            "  \"expiresIn\": 3600\n" +
                                            "}"
                            )
                    )
            )
            @RequestBody TokensResponse r
    ) {
        try {
            TokensResponse t = svc.refresh(r.refreshToken());
            return ResponseEntity.ok(Map.of("estado", 1, "mensaje", "Token renovado", "tokens", t));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("estado", 0, "mensaje", "Token inválido/expirado"));
        }
    }

    // ---------- LOGOUT (estatutario) ----------
    @PostMapping("/logout")
    @Operation(
            summary = "Cerrar sesión",
            description = "Endpoint estatutario para cerrar sesión en el cliente (invalidación lógica del lado del frontend)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Sesión cerrada correctamente en el cliente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Logout exitoso",
                                    value = "{\n" +
                                            "  \"estado\": 1,\n" +
                                            "  \"mensaje\": \"Sesión cerrada\"\n" +
                                            "}"
                            )
                    )
            )
    })
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("estado", 1, "mensaje", "Sesión cerrada"));
    }
}
