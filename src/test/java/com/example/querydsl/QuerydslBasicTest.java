package com.example.querydsl;

import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory; // 멀티스레드 환경에서 동시성 문제가 없이 설계되어 있다.

    @BeforeEach // 각 테스트 케이스전에 실행한다.
    public void before(){
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        // member1을 찾아라
        String qlString =
                "select m from Member m " +
                "where m.username = :username";

        // 첫 번째 인자로 JPQL 문자열(qlString)을 받고, 두 번째 인자로 결과로 받을 엔티티 타입을 지정합니다.
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        // .getSingleResult() 는
        // 실행된 쿼리의 결과를 단일 객체 하나로 반환하는 메서드입니다.
        // 결과가 정확히 1건일 때 사용합니다.
        // 만약 결과가 없으면 NoResultException
        // 2건 이상이면 NonUniqueResultException 예외가 발생합니다.
        // 참고로 여러 건을 가져오려면 .getResultList()를 사용합니다.

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        // 참고로 gradle 사용시 Querydsl 사용하려면 gradle - Task - other - compileQuerydsl 을 실행해주면
        // build 파일에 querydsl 관련 entity가 생성된다.
        QMember m = new QMember("m");

        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) // 파라미터 바인딩을 자동으로 해준다.
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
}
