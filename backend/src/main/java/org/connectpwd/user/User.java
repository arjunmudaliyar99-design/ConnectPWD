package org.connectpwd.user;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    @Field("full_name")
    private String fullName;

    @Indexed(unique = true)
    private String email;

    @Field("password_hash")
    private String passwordHash;

    @Builder.Default
    private UserRole role = UserRole.CAREGIVER;

    private String phone;

    @Builder.Default
    private String language = "en";

    @Field("is_active")
    @Builder.Default
    private boolean isActive = true;

    @CreatedDate
    @Field("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Field("updated_at")
    private Instant updatedAt;
}
