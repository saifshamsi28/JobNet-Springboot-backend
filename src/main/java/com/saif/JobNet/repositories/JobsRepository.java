package com.saif.JobNet.repositories;

import com.saif.JobNet.model.Job;
import org.springframework.data.domain.Example;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.function.Function;

@Repository
public interface JobsRepository extends MongoRepository<Job,String> {
    @Query("{ '$and': [ " +
            "{ '$or': [ { 'title': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }, " +
            "{ '$or': [ { 'location': { $regex: ?1, $options: 'i' } }, { '?1': null } ] }, " +
            "{ '$or': [ { 'company': { $regex: ?2, $options: 'i' } }, { '?2': null } ] }, " +
            "{ '$or': [ { 'salary': { $gte: ?3 } }, { '?3': 0 } ] }, " +
            "{ '$or': [ { 'jobType': { $regex: ?4, $options: 'i' } }, { '?4': null } ] } " +
            "] }")
    List<Job> searchJobsByFilters(String title, String location, String company, int minSalary, String jobType);
}
//    @Override
//    <S extends Job, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction);

