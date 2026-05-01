package org.connectpwd.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDTO {
    private String id;
    private String fullName;
    private String email;
    private String role;
    private String language;
}
