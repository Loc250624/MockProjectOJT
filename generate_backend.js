const fs = require('fs');
const path = require('path');

const baseDir = 'd:/Study/FPT-KY6/MockProject/Code/src/main/java/com/ojtsu26/elearning';
const dirs = ['model/enums', 'model/entity', 'dto/request', 'dto/response', 'mapper', 'repository', 'service', 'service/impl'];

dirs.forEach(dir => {
    const fullPath = path.join(baseDir, dir);
    if (!fs.existsSync(fullPath)) {
        fs.mkdirSync(fullPath, { recursive: true });
    }
});

const enums = [
    { name: 'Role', values: ['STUDENT', 'TEACHER', 'ADMIN'] },
    { name: 'AuthProvider', values: ['LOCAL', 'GOOGLE', 'GITHUB'] },
    { name: 'UserStatus', values: ['ACTIVE', 'BLOCKED'] },
    { name: 'CourseStatus', values: ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'HIDDEN'] },
    { name: 'LessonType', values: ['VIDEO', 'QUIZ', 'CODING'] },
    { name: 'SubmissionStatus', values: ['PENDING_REVIEW', 'PASSED', 'FAILED'] },
    { name: 'PaymentMethod', values: ['MOMO', 'VNPAY'] },
    { name: 'TransactionStatus', values: ['PENDING', 'SUCCESS', 'FAILED', 'REFUNDED'] },
    { name: 'BlogStatus', values: ['PENDING', 'APPROVED', 'REJECTED'] }
];

enums.forEach(e => {
    const content = `package com.ojtsu26.elearning.model.enums;

public enum ${e.name} {
    ${e.values.join(',\n    ')}
}
`;
    fs.writeFileSync(path.join(baseDir, 'model/enums', `${e.name}.java`), content);
});

const entities = [
    {
        name: 'User', table: 'Users',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'fullName' },
            { type: 'String', name: 'email', isUnique: true },
            { type: 'String', name: 'passwordHash' },
            { type: 'String', name: 'avatarUrl' },
            { type: 'Role', name: 'role', isEnum: true },
            { type: 'AuthProvider', name: 'authProvider', isEnum: true },
            { type: 'UserStatus', name: 'status', isEnum: true },
            { type: 'java.time.LocalDateTime', name: 'createdAt', isCreate: true },
            { type: 'java.time.LocalDateTime', name: 'updatedAt', isUpdate: true }
        ],
        relations: [
            { type: 'OneToMany', target: 'Roadmap', mappedBy: 'instructor' },
            { type: 'OneToMany', target: 'Course', mappedBy: 'instructor' },
            { type: 'OneToMany', target: 'CourseEnrollment', mappedBy: 'student' },
            { type: 'OneToMany', target: 'Submission', mappedBy: 'student' },
            { type: 'OneToMany', target: 'Certificate', mappedBy: 'student' },
            { type: 'OneToMany', target: 'Transaction', mappedBy: 'student' },
            { type: 'OneToMany', target: 'Blog', mappedBy: 'author' },
            { type: 'OneToMany', target: 'Comment', mappedBy: 'user' }
        ]
    },
    {
        name: 'Category', table: 'Categories',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'name' },
            { type: 'String', name: 'description' }
        ],
        relations: [
            { type: 'OneToMany', target: 'Course', mappedBy: 'category' }
        ]
    },
    {
        name: 'Roadmap', table: 'Roadmaps',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'title' },
            { type: 'String', name: 'description' },
            { type: 'java.time.LocalDateTime', name: 'createdAt', isCreate: true }
        ],
        relations: [
            { type: 'ManyToOne', target: 'User', name: 'instructor', joinColumn: 'instructor_id' },
            { type: 'OneToMany', target: 'Course', mappedBy: 'roadmap' }
        ]
    },
    {
        name: 'Course', table: 'Courses',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'title' },
            { type: 'String', name: 'description' },
            { type: 'String', name: 'thumbnailUrl' },
            { type: 'java.math.BigDecimal', name: 'price' },
            { type: 'CourseStatus', name: 'status', isEnum: true },
            { type: 'java.time.LocalDateTime', name: 'createdAt', isCreate: true },
            { type: 'java.time.LocalDateTime', name: 'updatedAt', isUpdate: true }
        ],
        relations: [
            { type: 'ManyToOne', target: 'User', name: 'instructor', joinColumn: 'instructor_id' },
            { type: 'ManyToOne', target: 'Category', name: 'category', joinColumn: 'category_id' },
            { type: 'ManyToOne', target: 'Roadmap', name: 'roadmap', joinColumn: 'roadmap_id' },
            { type: 'OneToMany', target: 'Lesson', mappedBy: 'course' },
            { type: 'OneToMany', target: 'CourseEnrollment', mappedBy: 'course' },
            { type: 'OneToMany', target: 'Certificate', mappedBy: 'course' },
            { type: 'OneToMany', target: 'Transaction', mappedBy: 'course' }
        ]
    },
    {
        name: 'Lesson', table: 'Lessons',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'title' },
            { type: 'String', name: 'content' },
            { type: 'LessonType', name: 'type', isEnum: true },
            { type: 'Integer', name: 'orderIndex' },
            { type: 'java.time.LocalDateTime', name: 'createdAt', isCreate: true }
        ],
        relations: [
            { type: 'ManyToOne', target: 'Course', name: 'course', joinColumn: 'course_id' },
            { type: 'OneToOne', target: 'Video', mappedBy: 'lesson' },
            { type: 'OneToOne', target: 'Quiz', mappedBy: 'lesson' },
            { type: 'OneToOne', target: 'CodingAssignment', mappedBy: 'lesson' },
            { type: 'OneToMany', target: 'LessonProgress', mappedBy: 'lesson' },
            { type: 'OneToMany', target: 'Submission', mappedBy: 'lesson' }
        ]
    },
    {
        name: 'Video', table: 'Videos',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'videoUrl' },
            { type: 'Integer', name: 'durationSeconds' }
        ],
        relations: [
            { type: 'OneToOne', target: 'Lesson', name: 'lesson', joinColumn: 'lesson_id' }
        ]
    },
    {
        name: 'Quiz', table: 'Quizzes',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'title' },
            { type: 'java.math.BigDecimal', name: 'passingScore' }
        ],
        relations: [
            { type: 'OneToOne', target: 'Lesson', name: 'lesson', joinColumn: 'lesson_id' },
            { type: 'OneToMany', target: 'Question', mappedBy: 'quiz' }
        ]
    },
    {
        name: 'Question', table: 'Questions',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'questionText' },
            { type: 'String', name: 'optionsJson' },
            { type: 'String', name: 'correctAnswer' }
        ],
        relations: [
            { type: 'ManyToOne', target: 'Quiz', name: 'quiz', joinColumn: 'quiz_id' }
        ]
    },
    {
        name: 'CodingAssignment', table: 'Coding_Assignments',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'title' },
            { type: 'String', name: 'problemStatement' },
            { type: 'String', name: 'allowedLanguages' },
            { type: 'Integer', name: 'timeLimitMs' }
        ],
        relations: [
            { type: 'OneToOne', target: 'Lesson', name: 'lesson', joinColumn: 'lesson_id' },
            { type: 'OneToMany', target: 'Testcase', mappedBy: 'assignment' }
        ]
    },
    {
        name: 'Testcase', table: 'Testcases',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'inputData' },
            { type: 'String', name: 'expectedOutput' },
            { type: 'Boolean', name: 'isHidden' }
        ],
        relations: [
            { type: 'ManyToOne', target: 'CodingAssignment', name: 'assignment', joinColumn: 'assignment_id' }
        ]
    },
    {
        name: 'CourseEnrollment', table: 'Course_Enrollments',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'java.math.BigDecimal', name: 'progressPercentage' },
            { type: 'Boolean', name: 'isCompleted' },
            { type: 'java.time.LocalDateTime', name: 'enrolledAt', isCreate: true }
        ],
        relations: [
            { type: 'ManyToOne', target: 'User', name: 'student', joinColumn: 'student_id' },
            { type: 'ManyToOne', target: 'Course', name: 'course', joinColumn: 'course_id' },
            { type: 'OneToMany', target: 'LessonProgress', mappedBy: 'enrollment' }
        ]
    },
    {
        name: 'LessonProgress', table: 'Lesson_Progress',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'Boolean', name: 'isCompleted' },
            { type: 'java.time.LocalDateTime', name: 'completedAt' }
        ],
        relations: [
            { type: 'ManyToOne', target: 'CourseEnrollment', name: 'enrollment', joinColumn: 'enrollment_id' },
            { type: 'ManyToOne', target: 'Lesson', name: 'lesson', joinColumn: 'lesson_id' }
        ]
    },
    {
        name: 'Submission', table: 'Submissions',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'java.math.BigDecimal', name: 'score' },
            { type: 'SubmissionStatus', name: 'status', isEnum: true },
            { type: 'String', name: 'submittedContent' },
            { type: 'String', name: 'teacherFeedback' },
            { type: 'java.time.LocalDateTime', name: 'submittedAt', isCreate: true }
        ],
        relations: [
            { type: 'ManyToOne', target: 'User', name: 'student', joinColumn: 'student_id' },
            { type: 'ManyToOne', target: 'Lesson', name: 'lesson', joinColumn: 'lesson_id' }
        ]
    },
    {
        name: 'Certificate', table: 'Certificates',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'java.time.LocalDateTime', name: 'issueDate', isCreate: true },
            { type: 'String', name: 'certificateUrl' }
        ],
        relations: [
            { type: 'ManyToOne', target: 'User', name: 'student', joinColumn: 'student_id' },
            { type: 'ManyToOne', target: 'Course', name: 'course', joinColumn: 'course_id' }
        ]
    },
    {
        name: 'Transaction', table: 'Transactions',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'java.math.BigDecimal', name: 'amount' },
            { type: 'PaymentMethod', name: 'paymentMethod', isEnum: true },
            { type: 'String', name: 'transactionRef', isUnique: true },
            { type: 'TransactionStatus', name: 'status', isEnum: true },
            { type: 'String', name: 'webhookResponse' },
            { type: 'java.time.LocalDateTime', name: 'createdAt', isCreate: true },
            { type: 'java.time.LocalDateTime', name: 'updatedAt', isUpdate: true }
        ],
        relations: [
            { type: 'ManyToOne', target: 'User', name: 'student', joinColumn: 'student_id' },
            { type: 'ManyToOne', target: 'Course', name: 'course', joinColumn: 'course_id' }
        ]
    },
    {
        name: 'Blog', table: 'Blogs',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'title' },
            { type: 'String', name: 'content' },
            { type: 'BlogStatus', name: 'status', isEnum: true },
            { type: 'String', name: 'rejectReason' },
            { type: 'java.time.LocalDateTime', name: 'createdAt', isCreate: true },
            { type: 'java.time.LocalDateTime', name: 'updatedAt', isUpdate: true }
        ],
        relations: [
            { type: 'ManyToOne', target: 'User', name: 'author', joinColumn: 'author_id' }
        ]
    },
    {
        name: 'Comment', table: 'Comments',
        fields: [
            { type: 'Integer', name: 'id', isId: true },
            { type: 'String', name: 'targetType' },
            { type: 'Integer', name: 'targetId' },
            { type: 'String', name: 'content' },
            { type: 'java.time.LocalDateTime', name: 'createdAt', isCreate: true }
        ],
        relations: [
            { type: 'ManyToOne', target: 'User', name: 'user', joinColumn: 'user_id' },
            { type: 'ManyToOne', target: 'Comment', name: 'parent', joinColumn: 'parent_id' },
            { type: 'OneToMany', target: 'Comment', mappedBy: 'parent' }
        ]
    }
];

// Entity Generation
entities.forEach(ent => {
    let imports = `import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.ojtsu26.elearning.model.enums.*;
import java.util.List;
`;
    let fields = [];
    
    ent.fields.forEach(f => {
        let annos = [];
        if (f.isId) annos.push('@Id\n    @GeneratedValue(strategy = GenerationType.IDENTITY)');
        if (f.isCreate) annos.push('@CreationTimestamp');
        if (f.isUpdate) annos.push('@UpdateTimestamp');
        if (f.isEnum) annos.push('@Enumerated(EnumType.STRING)');
        if (f.isUnique) annos.push('@Column(unique = true)');
        fields.push(`    ${annos.join('\n    ')}\n    private ${f.type} ${f.name};`);
    });

    ent.relations.forEach(r => {
        let annos = [];
        if (r.type === 'ManyToOne') {
            annos.push(`@ManyToOne\n    @JoinColumn(name = "${r.joinColumn}")\n    @JsonBackReference("${ent.name.toLowerCase()}-${r.name}")`);
            fields.push(`    ${annos.join('\n    ')}\n    private ${r.target} ${r.name};`);
        } else if (r.type === 'OneToMany') {
            annos.push(`@OneToMany(mappedBy = "${r.mappedBy}", cascade = CascadeType.ALL, orphanRemoval = true)\n    @JsonManagedReference("${r.target.toLowerCase()}-${r.mappedBy}")`);
            fields.push(`    ${annos.join('\n    ')}\n    private List<${r.target}> ${r.target.toLowerCase()}s;`);
        } else if (r.type === 'OneToOne') {
            if (r.mappedBy) {
                annos.push(`@OneToOne(mappedBy = "${r.mappedBy}", cascade = CascadeType.ALL, orphanRemoval = true)\n    @JsonManagedReference("${r.target.toLowerCase()}-${r.mappedBy}")`);
            } else {
                annos.push(`@OneToOne\n    @JoinColumn(name = "${r.joinColumn}")\n    @JsonBackReference("${ent.name.toLowerCase()}-${r.name}")`);
            }
            fields.push(`    ${annos.join('\n    ')}\n    private ${r.target} ${r.name || r.target.toLowerCase()};`);
        }
    });

    let content = `package com.ojtsu26.elearning.model.entity;

${imports}
@Entity
@Table(name = "${ent.table}")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ${ent.name} {

${fields.join('\n\n')}
}
`;
    fs.writeFileSync(path.join(baseDir, 'model/entity', `${ent.name}.java`), content);
});

// DTOs Generation
entities.forEach(ent => {
    let baseFields = ent.fields.filter(f => !f.isId && !f.isCreate && !f.isUpdate);
    let reqFields = baseFields.map(f => {
        let annos = f.type === 'String' ? '@jakarta.validation.constraints.NotBlank' : '@jakarta.validation.constraints.NotNull';
        return `    ${annos}\n    private ${f.type} ${f.name};`;
    });
    
    ent.relations.filter(r => r.type === 'ManyToOne' || (r.type === 'OneToOne' && r.joinColumn)).forEach(r => {
        reqFields.push(`    @jakarta.validation.constraints.NotNull\n    private Integer ${r.name}Id;`);
    });

    let reqContent = `package com.ojtsu26.elearning.dto.request;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class ${ent.name}RequestDTO {
${reqFields.join('\n\n')}
}
`;
    fs.writeFileSync(path.join(baseDir, 'dto/request', `${ent.name}RequestDTO.java`), reqContent);

    let resFields = ent.fields.map(f => `    private ${f.type} ${f.name};`);
    ent.relations.filter(r => r.type === 'ManyToOne' || (r.type === 'OneToOne' && r.joinColumn)).forEach(r => {
        resFields.push(`    private Integer ${r.name}Id;`);
    });

    let resContent = `package com.ojtsu26.elearning.dto.response;

import lombok.Data;
import com.ojtsu26.elearning.model.enums.*;

@Data
public class ${ent.name}ResponseDTO {
${resFields.join('\n\n')}
}
`;
    fs.writeFileSync(path.join(baseDir, 'dto/response', `${ent.name}ResponseDTO.java`), resContent);
});

// Mapper Generation
entities.forEach(ent => {
    let mappingAnnos = ent.relations.filter(r => r.type === 'ManyToOne' || (r.type === 'OneToOne' && r.joinColumn)).map(r => 
        `    @Mapping(source = "${r.name}.id", target = "${r.name}Id")`
    ).join('\n');

    let reqMappingAnnos = ent.relations.filter(r => r.type === 'ManyToOne' || (r.type === 'OneToOne' && r.joinColumn)).map(r => 
        `    @Mapping(source = "${r.name}Id", target = "${r.name}.id")`
    ).join('\n');

    let content = `package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.model.entity.${ent.name};
import com.ojtsu26.elearning.dto.request.${ent.name}RequestDTO;
import com.ojtsu26.elearning.dto.response.${ent.name}ResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ${ent.name}Mapper {

${mappingAnnos}
    ${ent.name}ResponseDTO toDto(${ent.name} entity);

${reqMappingAnnos}
    ${ent.name} toEntity(${ent.name}RequestDTO dto);
}
`;
    fs.writeFileSync(path.join(baseDir, 'mapper', `${ent.name}Mapper.java`), content);
});

// Repository Generation
entities.forEach(ent => {
    let content = `package com.ojtsu26.elearning.repository;

import com.ojtsu26.elearning.model.entity.${ent.name};
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ${ent.name}Repository extends JpaRepository<${ent.name}, Integer> {
}
`;
    fs.writeFileSync(path.join(baseDir, 'repository', `${ent.name}Repository.java`), content);
});

// Service Generation
entities.forEach(ent => {
    let content = `package com.ojtsu26.elearning.service;

import com.ojtsu26.elearning.dto.request.${ent.name}RequestDTO;
import com.ojtsu26.elearning.dto.response.${ent.name}ResponseDTO;
import java.util.List;

public interface ${ent.name}Service {
    List<${ent.name}ResponseDTO> findAll();
    ${ent.name}ResponseDTO findById(Integer id);
    ${ent.name}ResponseDTO create(${ent.name}RequestDTO requestDTO);
    ${ent.name}ResponseDTO update(Integer id, ${ent.name}RequestDTO requestDTO);
    void delete(Integer id);
}
`;
    fs.writeFileSync(path.join(baseDir, 'service', `${ent.name}Service.java`), content);
});

// ServiceImpl Generation
entities.forEach(ent => {
    let lowerName = ent.name.charAt(0).toLowerCase() + ent.name.slice(1);
    let content = `package com.ojtsu26.elearning.service.impl;

import com.ojtsu26.elearning.model.entity.${ent.name};
import com.ojtsu26.elearning.dto.request.${ent.name}RequestDTO;
import com.ojtsu26.elearning.dto.response.${ent.name}ResponseDTO;
import com.ojtsu26.elearning.mapper.${ent.name}Mapper;
import com.ojtsu26.elearning.repository.${ent.name}Repository;
import com.ojtsu26.elearning.service.${ent.name}Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ${ent.name}ServiceImpl implements ${ent.name}Service {

    private final ${ent.name}Repository ${lowerName}Repository;
    private final ${ent.name}Mapper ${lowerName}Mapper;

    @Override
    public List<${ent.name}ResponseDTO> findAll() {
        return ${lowerName}Repository.findAll().stream()
                .map(${lowerName}Mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ${ent.name}ResponseDTO findById(Integer id) {
        ${ent.name} entity = ${lowerName}Repository.findById(id)
                .orElseThrow(() -> new RuntimeException("${ent.name} not found"));
        return ${lowerName}Mapper.toDto(entity);
    }

    @Override
    public ${ent.name}ResponseDTO create(${ent.name}RequestDTO requestDTO) {
        ${ent.name} entity = ${lowerName}Mapper.toEntity(requestDTO);
        ${ent.name} saved = ${lowerName}Repository.save(entity);
        return ${lowerName}Mapper.toDto(saved);
    }

    @Override
    public ${ent.name}ResponseDTO update(Integer id, ${ent.name}RequestDTO requestDTO) {
        if (!${lowerName}Repository.existsById(id)) {
            throw new RuntimeException("${ent.name} not found");
        }
        ${ent.name} entity = ${lowerName}Mapper.toEntity(requestDTO);
        entity.setId(id);
        ${ent.name} updated = ${lowerName}Repository.save(entity);
        return ${lowerName}Mapper.toDto(updated);
    }

    @Override
    public void delete(Integer id) {
        ${lowerName}Repository.deleteById(id);
    }
}
`;
    fs.writeFileSync(path.join(baseDir, 'service/impl', `${ent.name}ServiceImpl.java`), content);
});

console.log('All backend files generated successfully!');
