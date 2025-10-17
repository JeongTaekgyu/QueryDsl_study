package com.example.querydsl.entity;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자를 PROTECTED 레벨로 생성
@ToString(of = {"of", "name"}) // 무한 참조 될 수 있기 때문에 연관관계 있는 필드는 제외하고 등록하자
public class Team {

    @Id @GeneratedValue
    @Column(name = "team_id")
    private Long id;

    private String name;

    // 양방향 연관관계의 주인은 Member에 있는 team이다.
    @OneToMany(mappedBy = "team")
    private List<Member> members = new ArrayList<>();

    public Team(String name) {
        this.name = name;
    }

}