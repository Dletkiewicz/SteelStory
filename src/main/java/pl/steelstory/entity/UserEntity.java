package pl.steelstory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.NaturalId;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import pl.steelstory.model.UserDto;

import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id")
  private UUID databaseId;

  @NaturalId
  @Column(name = "business_id")
  private UUID businessId;

  @Column(name = "username")
  private String username;

  @Column(name = "password")
  private String password;

  @Embedded
  private final AuditMixin audit = new AuditMixin();

  public UserDto toModel() {
    return new UserDto(businessId, username, password);
  }
}
