package pl.steelstory.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
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

}
