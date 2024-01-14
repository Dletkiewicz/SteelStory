package pl.steelstory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Embeddable
public class AuditMixin {

  @CreatedDate
  @Column(name = "created_at")
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "last_modified_at")
  private Instant lastModifiedAt;
}
