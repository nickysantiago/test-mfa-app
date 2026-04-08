package main.java.com.example.mfademo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpMethod;


@Controller
public class BambooHrOAuthController {

    @Value("${bamboohr.client-id}")
    private String clientId;

    @Value("${bamboohr.client-secret}")
    private String clientSecret;

    @Value("${bamboohr.redirect-uri}")
    private String redirectUri;

    @Value("${bamboohr.company-domain}")
    private String companyDomain;

    private final RestTemplate restTemplate = new RestTemplate();

    // Step 1: Redirect user to BambooHR authorization page
    @GetMapping("/bamboohr/connect")
    public void connect(HttpServletResponse response) throws IOException {
        String authUrl = "https://" + companyDomain + ".bamboohr.com/authorize.php" +
            "?request=authorize" +
            "&response_type=code" +
            "&state=bamboohr-state" +
            "&scope=employee_directory" +
            "&client_id=" + clientId +
            "&redirect_uri=" + redirectUri;

        response.sendRedirect(authUrl);
    }

    // Step 2: Handle the callback, exchange code for token, fetch directory
    @GetMapping("/bamboohr/callback")
    public String callback(@RequestParam("code") String code,
                           HttpServletRequest request) {

        // Exchange authorization code for access token
        String tokenUrl = "https://" + companyDomain + ".bamboohr.com/token.php?request=token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("code", code);
        body.put("grant_type", "authorization_code");
        body.put("redirect_uri", redirectUri);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, entity, Map.class);

        String accessToken = (String) tokenResponse.getBody().get("access_token");

        // Use access token to call the employee directory API
        String directoryUrl = "https://api.bamboohr.com/api/gateway.php/" 
            + companyDomain + "/v1/employees/directory";

        HttpHeaders apiHeaders = new HttpHeaders();
        apiHeaders.setBearerAuth(accessToken);
        apiHeaders.set("Accept", "application/json");

        HttpEntity<Void> apiRequest = new HttpEntity<>(apiHeaders);
        ResponseEntity<Map> directoryResponse = restTemplate.exchange(
            directoryUrl, HttpMethod.GET, apiRequest, Map.class);

        // Store directory in session for display
        request.getSession().setAttribute("bamboohr_directory", 
            directoryResponse.getBody().get("employees"));

        return "redirect:/directory";
    }
}
