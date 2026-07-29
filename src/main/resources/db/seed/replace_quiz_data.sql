-- LumiNa quiz data replacement seed
-- Target database: MySQL 8.0 / schema mock_project
--
-- Purpose:
--   * Replace the low-quality generated question banks for all 51 current quizzes.
--   * Remove obsolete attempts, answers, snapshots, and generation jobs that
--     refer to the replaced question banks.
--   * Insert ten reviewed single-choice questions per quiz (510 total) and a
--     difficulty-aware blueprint (4 EASY, 4 MEDIUM, 2 HARD).
--
-- Content approach:
--   Questions are newly written and paraphrased from the topic coverage and
--   assessment style of W3Schools quizzes/exercises and similar learning sources.
--   They are not a verbatim copy of any third-party quiz.
--
-- Reference starting points (reviewed 2026-07-29):
--   https://www.w3schools.com/html/html_quiz.asp
--   https://www.w3schools.com/css/css_quiz.asp
--   https://www.w3schools.com/js/js_quiz.asp
--   https://www.w3schools.com/python/python_quiz.asp
--   https://www.w3schools.com/sql/sql_quiz.asp
--   https://www.w3schools.com/react/react_quiz.asp
--   https://developer.mozilla.org/en-US/docs/Learn_web_development
--   https://docs.docker.com/get-started/
--   https://kubernetes.io/docs/tutorials/kubernetes-basics/
--
-- Run:
--   mysql --default-character-set=utf8mb4 -u <user> -p mock_project < replace_quiz_data.sql
--
-- IMPORTANT: this intentionally clears dedicated Quiz_Attempts history for the
-- targeted quizzes. Legacy Submissions and Lesson_Progress are preserved so the
-- replacement does not silently reset overall course progress.

SET NAMES utf8mb4;
SET @quiz_seed_started_at = CURRENT_TIMESTAMP(6);

DROP TEMPORARY TABLE IF EXISTS _quiz_targets;
CREATE TEMPORARY TABLE _quiz_targets (
    seed_key VARCHAR(50) NOT NULL PRIMARY KEY,
    course_title VARCHAR(255) NOT NULL,
    lesson_title VARCHAR(255) NOT NULL,
    topic_code VARCHAR(100) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO _quiz_targets (seed_key, course_title, lesson_title, topic_code) VALUES
('html_mid', 'HTML & CSS Master Course', 'HTML Knowledge Check', 'HTML_FOUNDATIONS'),
('html_final', 'HTML & CSS Master Course', 'HTML & CSS Course Final Assessment', 'HTML_CSS'),
('javascript', 'JavaScript: Zero to Hero', 'JavaScript: Zero to Hero Final Assessment', 'JAVASCRIPT'),
('react', 'ReactJS: Building Single Page Applications', 'ReactJS: Building Single Page Applications Final Assessment', 'REACT'),
('vue', 'Vue.js 3 in Action', 'Vue.js 3 in Action Final Assessment', 'VUE'),
('angular', 'Angular: Enterprise Frontend Framework', 'Angular: Enterprise Frontend Framework Final Assessment', 'ANGULAR'),
('typescript', 'Advanced TypeScript', 'Advanced TypeScript Final Assessment', 'TYPESCRIPT'),
('java', 'Java: Object-Oriented Programming', 'Java: Object-Oriented Programming Final Assessment', 'JAVA_OOP'),
('spring_boot', 'Spring Boot: Professional Backend APIs', 'Spring Boot: Professional Backend APIs Final Assessment', 'SPRING_BOOT'),
('csharp', 'C# Programming Fundamentals', 'C# Programming Fundamentals Final Assessment', 'CSHARP'),
('aspnet', 'ASP.NET Core Web Development', 'ASP.NET Core Web Development Final Assessment', 'ASPNET_CORE'),
('python', 'Python Programming Complete Guide', 'Python Programming Complete Guide Final Assessment', 'PYTHON'),
('django', 'Django Web Framework', 'Django Web Framework Final Assessment', 'DJANGO'),
('node', 'Node.js & Express.js Backend', 'Node.js & Express.js Backend Final Assessment', 'NODE_EXPRESS'),
('rest_api', 'REST API Design & Development', 'REST API Design & Development Final Assessment', 'REST_API'),
('microservices', 'Microservices Architecture', 'Microservices Architecture Final Assessment', 'MICROSERVICES'),
('dsa', 'Data Structures & Algorithms', 'Data Structures & Algorithms Final Assessment', 'DSA'),
('sql_mysql', 'SQL & MySQL: Basic to Advanced', 'SQL & MySQL: Basic to Advanced Final Assessment', 'SQL_MYSQL'),
('postgresql', 'PostgreSQL Administration', 'PostgreSQL Administration Final Assessment', 'POSTGRESQL'),
('mongodb', 'MongoDB: NoSQL Database', 'MongoDB: NoSQL Database Final Assessment', 'MONGODB'),
('redis', 'Redis Caching & Performance', 'Redis Caching & Performance Final Assessment', 'REDIS'),
('docker', 'Docker: Container Fundamentals', 'Docker: Container Fundamentals Final Assessment', 'DOCKER'),
('kubernetes', 'Kubernetes Orchestration', 'Kubernetes Orchestration Final Assessment', 'KUBERNETES'),
('aws', 'AWS Cloud Practitioner', 'AWS Cloud Practitioner Final Assessment', 'AWS'),
('azure', 'Microsoft Azure Fundamentals', 'Microsoft Azure Fundamentals Final Assessment', 'AZURE'),
('gcp', 'Google Cloud Platform Essentials', 'Google Cloud Platform Essentials Final Assessment', 'GCP'),
('git_github', 'Git & GitHub Collaboration', 'Git & GitHub Collaboration Final Assessment', 'GIT_GITHUB'),
('cicd', 'CI/CD with Jenkins & GitHub Actions', 'CI/CD with Jenkins & GitHub Actions Final Assessment', 'CICD'),
('terraform', 'Terraform: Infrastructure as Code', 'Terraform: Infrastructure as Code Final Assessment', 'TERRAFORM'),
('uiux', 'UI/UX Design Principles', 'UI/UX Design Principles Final Assessment', 'UI_UX'),
('figma', 'Figma: Professional UI Design', 'Figma: Professional UI Design Final Assessment', 'FIGMA'),
('android_kotlin', 'Android Development with Kotlin', 'Android Development with Kotlin Final Assessment', 'ANDROID_KOTLIN'),
('flutter', 'Flutter Cross-Platform Development', 'Flutter Cross-Platform Development Final Assessment', 'FLUTTER'),
('react_native', 'React Native Mobile Apps', 'React Native Mobile Apps Final Assessment', 'REACT_NATIVE'),
('swift_ios', 'Swift & iOS Development', 'Swift & iOS Development Final Assessment', 'SWIFT_IOS'),
('machine_learning', 'Machine Learning with Python', 'Machine Learning with Python Final Assessment', 'MACHINE_LEARNING'),
('deep_learning', 'Deep Learning & Neural Networks', 'Deep Learning & Neural Networks Final Assessment', 'DEEP_LEARNING'),
('ai', 'AI Fundamentals & Applications', 'AI Fundamentals & Applications Final Assessment', 'AI'),
('pandas', 'Data Analysis with Pandas', 'Data Analysis with Pandas Final Assessment', 'PANDAS'),
('matplotlib', 'Data Visualization with Matplotlib', 'Data Visualization with Matplotlib Final Assessment', 'MATPLOTLIB'),
('selenium', 'Selenium Test Automation', 'Selenium Test Automation Final Assessment', 'SELENIUM'),
('junit_mockito', 'JUnit & Mockito Testing', 'JUnit & Mockito Testing Final Assessment', 'JUNIT_MOCKITO'),
('cybersecurity', 'Cybersecurity Fundamentals', 'Cybersecurity Fundamentals Final Assessment', 'CYBERSECURITY'),
('ethical_hacking', 'Ethical Hacking & Penetration Testing', 'Ethical Hacking & Penetration Testing Final Assessment', 'ETHICAL_HACKING'),
('linux', 'Linux System Administration', 'Linux System Administration Final Assessment', 'LINUX'),
('networking', 'Networking Fundamentals (CCNA)', 'Networking Fundamentals (CCNA) Final Assessment', 'NETWORKING'),
('graphql', 'GraphQL API Development', 'GraphQL API Development Final Assessment', 'GRAPHQL'),
('websocket', 'WebSocket & Real-time Applications', 'WebSocket & Real-time Applications Final Assessment', 'WEBSOCKET'),
('agile_scrum', 'Agile & Scrum Project Management', 'Agile & Scrum Project Management Final Assessment', 'AGILE_SCRUM'),
('architecture', 'Software Architecture Patterns', 'Software Architecture Patterns Final Assessment', 'SOFTWARE_ARCHITECTURE'),
('blockchain', 'Blockchain & Web3 Development', 'Blockchain & Web3 Development Final Assessment', 'BLOCKCHAIN');

DROP TEMPORARY TABLE IF EXISTS _quiz_questions;
CREATE TEMPORARY TABLE _quiz_questions (
    seed_key VARCHAR(50) NOT NULL,
    question_order INT NOT NULL,
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL,
    question_text VARCHAR(255) NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255) NOT NULL,
    option_d VARCHAR(255) NOT NULL,
    correct_index TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (seed_key, question_order),
    CONSTRAINT chk_quiz_seed_correct_index CHECK (correct_index BETWEEN 0 AND 3)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

INSERT INTO _quiz_questions
    (seed_key, question_order, difficulty, question_text,
     option_a, option_b, option_c, option_d, correct_index)
VALUES
-- HTML checkpoint
('html_mid',1,'EASY','Which HTML element represents the largest heading?','<heading>','<h1>','<head>','<h6>',1),
('html_mid',2,'EASY','Which attribute supplies alternative text for an image?','title','href','alt','srcset',2),
('html_mid',3,'MEDIUM','Which element creates an ordered list?','<ol>','<ul>','<li>','<dl>',0),
('html_mid',4,'MEDIUM','What does the action attribute of a form specify?','The HTTP method','The submit destination','The input encoding only','The validation rule',1),
('html_mid',5,'HARD','Which markup gives a table header cell that spans two columns?','<th span="2">','<th colspan="2">','<td rowspan="2">','<thead cols="2">',1),

-- HTML and CSS final
('html_final',1,'EASY','Which CSS selector targets every element with class card?','#card','card','.card','*card',2),
('html_final',2,'EASY','Which declaration turns an element into a flex container?','position: flex','display: flex','layout: flex','flex: container',1),
('html_final',3,'MEDIUM','In the standard box model, what surrounds padding?','Content','Margin','Border','Outline only',2),
('html_final',4,'MEDIUM','Which semantic element is intended for primary site navigation links?','<nav>','<aside>','<section>','<main>',0),
('html_final',5,'HARD','Which media query applies styles at viewport widths of 768px or less?','@media (width >= 768px)','@media (max-width: 768px)','@media screen: 768px','@media (min-width: 768px)',1),

-- JavaScript
('javascript',1,'EASY','Which keyword declares a block-scoped variable that can be reassigned?','const','let','static','define',1),
('javascript',2,'EASY','What is the result type of typeof "42"?','number','string','object','undefined',1),
('javascript',3,'MEDIUM','Which array method creates a new array by transforming every element?','forEach','filter','map','find',2),
('javascript',4,'MEDIUM','What does an async function always return?','A callback','A Promise','A generator','A boolean',1),
('javascript',5,'HARD','What is logged by console.log(0 === false)?','true','false','undefined','TypeError',1),

-- React
('react',1,'EASY','What syntax lets JavaScript describe React UI elements?','SFC','JSX','DOMQL','XMLHttp',1),
('react',2,'EASY','How are values normally passed from a parent component to a child?','Props','Reducers','Refs only','Effects',0),
('react',3,'MEDIUM','Which Hook adds local state to a function component?','useEffect','useMemo','useState','useContext',2),
('react',4,'MEDIUM','Why should list items have stable keys?','To add CSS classes','To help reconciliation','To make props mutable','To enable routing',1),
('react',5,'HARD','When should useEffect return a cleanup function?','To render JSX','To update state synchronously','To release subscriptions or timers','To memoize a value',2),

-- Vue
('vue',1,'EASY','Which directive conditionally renders an element?','v-for','v-if','v-bind','v-slot',1),
('vue',2,'EASY','Which directive creates two-way binding for a form input?','v-model','v-once','v-html','v-show',0),
('vue',3,'MEDIUM','Which Composition API function creates a reactive value accessed through .value?','watch','computed','ref','provide',2),
('vue',4,'MEDIUM','What is the shorthand for v-on:click?','#:click','@:click','::click','&click',1),
('vue',5,'HARD','Which API derives a cached value from reactive dependencies?','computed','onMounted','reactive','nextTick',0),

-- Angular
('angular',1,'EASY','Which decorator marks a class as an Angular component?','@Module','@Component','@Inject','@View',1),
('angular',2,'EASY','Which syntax binds a component property to an element property?','(property)','[property]','{{#property}}','*property',1),
('angular',3,'MEDIUM','What is the main purpose of an Angular service?','Define CSS scope','Share reusable logic and data','Compile templates','Create database tables',1),
('angular',4,'MEDIUM','Which mechanism supplies a service instance to a component?','Event bubbling','Dependency injection','Template projection','Change detection',1),
('angular',5,'HARD','Which RxJS type represents a stream that can emit multiple values over time?','Promise','Observable','SignalR','Iterable only',1),

-- TypeScript
('typescript',1,'EASY','Which feature is added by TypeScript on top of JavaScript?','Static type checking','A new browser DOM','Automatic SQL support','A JVM runtime',0),
('typescript',2,'EASY','Which keyword defines the shape of an object type?','package','interface','namespaceOnly','prototype',1),
('typescript',3,'MEDIUM','What does the generic syntax function identity<T>(value: T): T preserve?','Only string values','The input type','Runtime reflection','Private access',1),
('typescript',4,'MEDIUM','Which utility type makes every property of T optional?','Readonly<T>','Pick<T>','Partial<T>','Record<T>',2),
('typescript',5,'HARD','What does the never type describe?','Any nullable value','A value that never occurs','An unknown value','An empty string',1),

-- Java
('java',1,'EASY','Which keyword creates a subclass in Java?','implements','extends','inherits','superclass',1),
('java',2,'EASY','Which access modifier restricts a field to its declaring class?','public','protected','private','package',2),
('java',3,'MEDIUM','What is method overloading?','Replacing a superclass method','Same name with different parameters','Calling a method recursively','Hiding every field',1),
('java',4,'MEDIUM','Which OOP principle exposes behavior while hiding internal state?','Encapsulation','Compilation','Serialization','Iteration',0),
('java',5,'HARD','When an overridden instance method is called through a superclass reference, which implementation runs?','Always the superclass version','The runtime object version','Both versions automatically','The compiler rejects it',1),

-- Spring Boot
('spring_boot',1,'EASY','Which annotation marks a class as a REST controller?','@Service','@RestController','@Entity','@RepositoryOnly',1),
('spring_boot',2,'EASY','Which annotation maps HTTP GET requests?','@GetMapping','@ReadMapping','@Select','@Fetch',0),
('spring_boot',3,'MEDIUM','What does dependency injection primarily reduce?','HTTP status codes','Tight coupling','Database indexes','JSON size',1),
('spring_boot',4,'MEDIUM','Which Spring Data interface commonly provides basic CRUD operations?','Runnable','CrudRepository','ServletContext','BeanFactoryPostProcessor',1),
('spring_boot',5,'HARD','Why use @Transactional on a service operation?','To enable CORS','To make grouped database work atomic','To serialize every response','To create a REST route',1),

-- C#
('csharp',1,'EASY','Which keyword creates a new object instance in C#?','make','new','alloc','instance',1),
('csharp',2,'EASY','Which type stores true or false?','bool','bit only','flag','binary',0),
('csharp',3,'MEDIUM','What is a property commonly used for in C#?','Controlled access to object data','Importing namespaces','Declaring assemblies','Starting threads only',0),
('csharp',4,'MEDIUM','Which keyword declares a class that cannot be instantiated directly?','sealed','abstract','staticOnly','internal',1),
('csharp',5,'HARD','What is the effect of using virtual on a base-class method?','It becomes private','Derived classes may override it','It runs at compile time only','It cannot return a value',1),

-- ASP.NET Core
('aspnet',1,'EASY','Which object is used to configure an ASP.NET Core application at startup?','WebApplicationBuilder','DataTable','HttpClientFactoryOnly','ViewBag',0),
('aspnet',2,'EASY','Which attribute maps a controller action to an HTTP GET route?','[HttpRead]','[HttpGet]','[RouteGetOnly]','[Select]',1),
('aspnet',3,'MEDIUM','What is middleware in ASP.NET Core?','A database row','A component in the HTTP pipeline','A Razor variable','A C# compiler option',1),
('aspnet',4,'MEDIUM','Which EF Core method persists tracked changes?','CommitEntity()','SaveChanges()','FlushModel()','WriteDb()',1),
('aspnet',5,'HARD','Why register a DbContext with scoped lifetime in a web app?','One instance per request is typical','It becomes a global singleton','It disables tracking','It removes dependency injection',0),

-- Python
('python',1,'EASY','Which keyword defines a function in Python?','func','def','function','lambdaOnly',1),
('python',2,'EASY','Which collection is ordered and mutable?','tuple','list','frozenset','range',1),
('python',3,'MEDIUM','What does [x * 2 for x in values] create?','A generator only','A list','A dictionary','A tuple',1),
('python',4,'MEDIUM','Which special method initializes a newly created instance?','__start__','__init__','__main__','__newclass__',1),
('python',5,'HARD','What does the with statement primarily help manage?','Only loops','Resource cleanup via context managers','Class inheritance','Package installation',1),

-- Django
('django',1,'EASY','Which Django component maps URL paths to view callables?','URLconf','Model manager','Template filter','Migration file',0),
('django',2,'EASY','Which command creates migration files from model changes?','migrate --new','makemigrations','collectstatic','startapp --db',1),
('django',3,'MEDIUM','What does a Django model normally represent?','A database-backed data structure','A CSS component','An HTTP server process','A template block',0),
('django',4,'MEDIUM','Which QuerySet method retrieves all matching rows?','objects.fetch()','objects.all()','objects.read()','objects.rows()',1),
('django',5,'HARD','Why should POST forms include a CSRF token?','To compress HTML','To prevent forged cross-site requests','To create database indexes','To enable template inheritance',1),

-- Node.js and Express
('node',1,'EASY','What is npm primarily used for?','Managing JavaScript packages','Compiling the Linux kernel','Designing database schemas','Rendering CSS only',0),
('node',2,'EASY','Which Express method registers a GET route?','app.read()','app.get()','app.select()','app.routeGetOnly()',1),
('node',3,'MEDIUM','What is Express middleware given access to?','Only the database','Request, response, and next','Only static HTML','The browser DOM',1),
('node',4,'MEDIUM','Why use await with a Promise in an async function?','To block the operating system','To resume after settlement','To convert it to CSS','To create a worker thread',1),
('node',5,'HARD','Where should Express error-handling middleware usually be registered?','Before every route only','After routes and other middleware','Inside package.json','In the browser',1),

-- REST API
('rest_api',1,'EASY','Which HTTP method is normally used to retrieve a resource?','GET','POST','PATCH','DELETE',0),
('rest_api',2,'EASY','Which status code means a resource was created successfully?','200','201','204','404',1),
('rest_api',3,'MEDIUM','What does idempotent mean for an HTTP operation?','It always returns JSON','Repeating it has the same intended effect','It requires authentication','It never changes state',1),
('rest_api',4,'MEDIUM','Which header tells the server the media type of the request body?','Accept-Language','Content-Type','Location','Cache-Control',1),
('rest_api',5,'HARD','Which response best handles a successful DELETE with no response body?','201 Created','204 No Content','302 Found','409 Conflict',1),

-- Microservices
('microservices',1,'EASY','What is a core microservices characteristic?','One deployable for all features','Independently deployable services','One shared process only','No network communication',1),
('microservices',2,'EASY','What does service discovery help a client find?','A source-code branch','A service network location','A database password','A UI color token',1),
('microservices',3,'MEDIUM','Why is a database per service often preferred?','To reduce service autonomy','To avoid tight data coupling','To remove transactions entirely','To force one schema',1),
('microservices',4,'MEDIUM','What is an API gateway commonly responsible for?','Compiling Java','Routing external requests','Replacing every database','Editing container images',1),
('microservices',5,'HARD','Which pattern stops repeated calls to an unhealthy dependency temporarily?','Factory','Circuit breaker','Observer','Repository',1),

-- Data structures and algorithms
('dsa',1,'EASY','Which data structure follows last in, first out?','Queue','Stack','Heap','Graph',1),
('dsa',2,'EASY','What is binary search time complexity on sorted data?','O(1)','O(log n)','O(n)','O(n squared)',1),
('dsa',3,'MEDIUM','Which structure commonly provides average O(1) key lookup?','Linked list','Hash table','Binary file','Stack only',1),
('dsa',4,'MEDIUM','Which traversal uses a queue to visit graph nodes level by level?','DFS','BFS','Binary search','Quicksort',1),
('dsa',5,'HARD','What is merge sort worst-case time complexity?','O(log n)','O(n)','O(n log n)','O(n squared)',2),

-- SQL and MySQL
('sql_mysql',1,'EASY','Which clause filters rows before grouping?','HAVING','WHERE','ORDER BY','LIMIT',1),
('sql_mysql',2,'EASY','Which JOIN returns only rows matching in both tables?','LEFT JOIN','INNER JOIN','CROSS JOIN','FULL JOIN',1),
('sql_mysql',3,'MEDIUM','Which clause filters aggregated groups?','WHERE','HAVING','VALUES','DISTINCT ON',1),
('sql_mysql',4,'MEDIUM','What is the main purpose of a database index?','Encrypt every row','Speed up selected lookups','Replace constraints','Store backups',1),
('sql_mysql',5,'HARD','Which isolation issue occurs when a transaction rereads a row and sees a committed change?','Dirty read','Non-repeatable read','Lost schema','Dead column',1),

-- PostgreSQL
('postgresql',1,'EASY','Which PostgreSQL command-line client is commonly used for interactive SQL?','pgcli only','psql','sqlcmd','mongosh',1),
('postgresql',2,'EASY','Which SQL command displays an execution plan without running the statement?','DESCRIBE PLAN','EXPLAIN','SHOW QUERY','TRACE SQL',1),
('postgresql',3,'MEDIUM','What does VACUUM primarily reclaim or make reusable?','Dead tuple storage','User passwords','Network ports','SQL functions',0),
('postgresql',4,'MEDIUM','Which index type is the default and suitable for equality and range comparisons?','GIN','GiST','B-tree','BRIN only',2),
('postgresql',5,'HARD','What does MVCC allow PostgreSQL readers and writers to do?','Always block each other','Use row versions for concurrency','Skip transactions','Share one global lock',1),

-- MongoDB
('mongodb',1,'EASY','What format does MongoDB use for document storage internally?','CSV','BSON','XML only','YAML',1),
('mongodb',2,'EASY','Which operation retrieves documents from a collection?','find()','select()','readRows()','fetchTable()',0),
('mongodb',3,'MEDIUM','Which operator matches values greater than a given value?','$gt','$in','$eqOnly','$push',0),
('mongodb',4,'MEDIUM','What is the purpose of an aggregation pipeline?','Run staged data transformations','Create CSS bundles','Compile Java bytecode','Manage DNS',0),
('mongodb',5,'HARD','Which index supports efficient searches within array elements?','Multikey index','Cluster lock','Sequence index','Bitmap file only',0),

-- Redis
('redis',1,'EASY','Which Redis command retrieves the value of a string key?','READ','GET','FETCH','SELECT',1),
('redis',2,'EASY','What does an expiration time on a key control?','Its data type','When it is automatically removed','Its replication role','Its sort order',1),
('redis',3,'MEDIUM','Which Redis data type stores unique unordered members?','List','Set','Stream only','String array',1),
('redis',4,'MEDIUM','Which caching pattern loads data into the cache after a miss?','Cache-aside','Write-only log','Round robin','Two-phase commit',0),
('redis',5,'HARD','Why can Redis Pub/Sub be unsuitable for durable messaging?','Messages are not persisted for offline subscribers','It cannot send strings','It requires SQL','It has no channels',0),

-- Docker
('docker',1,'EASY','What is a Docker image?','A running process only','A read-only container template','A virtual network','A source repository',1),
('docker',2,'EASY','Which file contains instructions used to build an image?','compose.lock','Dockerfile','container.json','image.yaml only',1),
('docker',3,'MEDIUM','What does docker compose up normally do?','Define and start application services','Delete every image','Push code to Git','Open a Kubernetes cluster',0),
('docker',4,'MEDIUM','Why use a named volume?','To persist data outside a container writable layer','To expose a TCP port','To rename an image','To compile a binary',0),
('docker',5,'HARD','What is a key benefit of a multi-stage Docker build?','More running containers','A smaller final image','Automatic database sharding','Unlimited cache size',1),

-- Kubernetes
('kubernetes',1,'EASY','What is the smallest deployable unit in Kubernetes?','Cluster','Pod','Node pool','Namespace',1),
('kubernetes',2,'EASY','Which object maintains a desired number of replicated application Pods?','Deployment','Secret','ConfigMap','IngressClass only',0),
('kubernetes',3,'MEDIUM','What does a Kubernetes Service provide?','Stable network access to Pods','Container image builds','Source control','Persistent source code',0),
('kubernetes',4,'MEDIUM','Which probe determines whether a container should receive traffic?','Liveness probe','Readiness probe','Startup command','Audit probe',1),
('kubernetes',5,'HARD','What usually happens when a Deployment rolling update proceeds?','All Pods stop at once','New ReplicaSet Pods replace old ones gradually','The cluster is recreated','Services lose their IP permanently',1),

-- AWS
('aws',1,'EASY','Which AWS service provides resizable virtual servers?','S3','EC2','RDS','Route 53',1),
('aws',2,'EASY','Which service is designed for object storage?','S3','Lambda','IAM','CloudWatch Logs only',0),
('aws',3,'MEDIUM','What is the principle of least privilege in IAM?','Grant only required permissions','Grant AdministratorAccess to all','Share root credentials','Disable audit logs',0),
('aws',4,'MEDIUM','Which AWS service offers managed relational databases?','RDS','SQS','ECR','CloudFront',0),
('aws',5,'HARD','What is an Availability Zone?','A billing account','An isolated location within a Region','A global IAM user','A single EC2 instance',1),

-- Azure
('azure',1,'EASY','Which Azure service hosts web apps without managing virtual machines directly?','App Service','Blob Indexer','Virtual Network only','Key Vault',0),
('azure',2,'EASY','What is an Azure resource group?','A logical container for resources','A physical disk','A user password','A DNS record only',0),
('azure',3,'MEDIUM','Which service stores unstructured objects such as images and backups?','Azure Blob Storage','Azure Functions','Azure DevOps Boards','Azure DNS',0),
('azure',4,'MEDIUM','What does Microsoft Entra ID primarily provide?','Identity and access management','Container image layers','SQL query planning','Static file compression',0),
('azure',5,'HARD','Why deploy resources across availability zones?','To reduce resiliency','To tolerate a datacenter-level failure','To use one fault domain','To avoid encryption',1),

-- Google Cloud
('gcp',1,'EASY','Which GCP service provides virtual machines?','Cloud Storage','Compute Engine','BigQuery','Cloud Run jobs only',1),
('gcp',2,'EASY','Which service stores immutable objects in buckets?','Cloud Storage','Cloud SQL','Pub/Sub','Cloud DNS',0),
('gcp',3,'MEDIUM','What is a GCP project used for?','Organizing resources, billing, and IAM','Defining one CSS page','Replacing every region','Storing passwords in source code',0),
('gcp',4,'MEDIUM','Which service is a managed relational database offering?','Cloud SQL','Cloud CDN','Cloud Build','Firestore only',0),
('gcp',5,'HARD','What distinguishes a zone from a region?','A zone is a deployment area within a region','A region is inside one VM','They are always identical','A zone is an IAM role',0),

-- Git and GitHub
('git_github',1,'EASY','Which command stages file changes for the next commit?','git add','git push','git clone','git status',0),
('git_github',2,'EASY','Which command creates a local copy of a remote repository?','git fork','git clone','git copy','git init-remote',1),
('git_github',3,'MEDIUM','What does a Git branch represent?','A movable pointer to a commit','A backup database','A user permission','A binary diff only',0),
('git_github',4,'MEDIUM','What is the purpose of a pull request?','Review and discuss proposed changes','Delete commit history','Install Git','Create a local stash only',0),
('git_github',5,'HARD','What does git rebase typically do to local commits?','Replays them onto a new base','Deletes the remote','Encrypts the repository','Creates merge commits only',0),

-- CI/CD
('cicd',1,'EASY','What does continuous integration encourage?','Frequent automated integration and testing','Annual releases only','Manual builds only','No version control',0),
('cicd',2,'EASY','Which file commonly defines a Jenkins pipeline?','Jenkinsfile','Docker.lock','pipeline.exe','workflow.json only',0),
('cicd',3,'MEDIUM','Where are GitHub Actions workflows stored?','.github/workflows','.git/actions-only','src/workflows','actions.ini',0),
('cicd',4,'MEDIUM','Why should a pipeline fail fast on test failure?','To stop promoting a known bad change','To skip logs','To delete all artifacts','To disable reviews',0),
('cicd',5,'HARD','What is an immutable build artifact?','The same tested artifact promoted between environments','A file edited after every deployment','A local-only source folder','A mutable production database',0),

-- Terraform
('terraform',1,'EASY','What language is commonly used for Terraform configuration?','HCL','SQL','JSX','XAML only',0),
('terraform',2,'EASY','Which command previews proposed infrastructure changes?','terraform show-only','terraform plan','terraform deploy','terraform inspect',1),
('terraform',3,'MEDIUM','What does Terraform state record?','The mapping between configuration and managed resources','Only provider passwords','Application log lines','Git commit messages',0),
('terraform',4,'MEDIUM','Why use a Terraform module?','To package reusable infrastructure configuration','To start a container runtime','To replace cloud IAM','To compile Java',0),
('terraform',5,'HARD','Why use remote state locking for team workflows?','To prevent concurrent state writes','To make all resources public','To disable plans','To store source images',0),

-- UI and UX
('uiux',1,'EASY','What does visual hierarchy help users understand?','The relative importance of content','The database schema','The source-control history','The network route',0),
('uiux',2,'EASY','What is a wireframe mainly used to communicate?','Page structure and layout','Final production code','Database indexes','Brand photography only',0),
('uiux',3,'MEDIUM','Which research method observes users performing representative tasks?','Usability testing','Color sampling','Unit testing','Load balancing',0),
('uiux',4,'MEDIUM','Why is sufficient color contrast important?','It improves readability and accessibility','It reduces HTTP requests','It creates database backups','It replaces labels',0),
('uiux',5,'HARD','What is the best validation for a redesigned checkout flow?','Task-based testing with target users','Designer preference only','More decorative icons','A larger source file',0),

-- Figma
('figma',1,'EASY','What does a Figma frame commonly represent?','A screen or layout container','A database transaction','A Git branch','A network socket',0),
('figma',2,'EASY','Why create a component?','To reuse a consistent UI element','To export a SQL schema','To create user accounts','To run unit tests',0),
('figma',3,'MEDIUM','What is an instance in Figma?','A linked use of a main component','A deleted layer','A bitmap export only','A page permission',0),
('figma',4,'MEDIUM','What does Auto Layout help manage?','Spacing, alignment, and responsive sizing','Database replication','HTTP caching','Source compilation',0),
('figma',5,'HARD','Why use component variants?','To group related states and configurations','To flatten every layer','To disable reuse','To replace prototypes with images',0),

-- Android and Kotlin
('android_kotlin',1,'EASY','Which Kotlin keyword declares a read-only reference?','var','val','constOnly','let',1),
('android_kotlin',2,'EASY','What is an Android Activity?','A component representing a user-facing screen','A database index','A Gradle repository','A network packet',0),
('android_kotlin',3,'MEDIUM','Which component efficiently displays a scrollable collection of items?','RecyclerView','BroadcastReceiver','ContentProvider only','Manifest',0),
('android_kotlin',4,'MEDIUM','What is an Intent commonly used for?','Requesting an action from another component','Drawing every layout','Compiling Kotlin','Creating SQL indexes',0),
('android_kotlin',5,'HARD','Why use a ViewModel for screen data?','It survives common configuration changes','It replaces the Activity UI','It stores secrets permanently','It runs only in XML',0),

-- Flutter
('flutter',1,'EASY','Which language is used to build Flutter applications?','Dart','Kotlin only','Swift only','Ruby',0),
('flutter',2,'EASY','What is a Widget in Flutter?','A building block of the UI','A database server','An app store certificate','A network protocol',0),
('flutter',3,'MEDIUM','When should a StatefulWidget be used?','When UI can change over time','When UI is always constant','Only for HTTP calls','Only for text',0),
('flutter',4,'MEDIUM','Which widget arranges children vertically?','Row','Column','StackOnly','GridTile',1),
('flutter',5,'HARD','Why should setState contain only the synchronous state change?','The framework can schedule a rebuild correctly','It starts a new isolate','It persists data automatically','It disables rendering',0),

-- React Native
('react_native',1,'EASY','Which component displays text in React Native?','<div>','<Text>','<p>','<label>',1),
('react_native',2,'EASY','Which component is the basic container for layout?','View','Section','Frame','ContainerOnly',0),
('react_native',3,'MEDIUM','How are styles commonly created in React Native?','StyleSheet.create','CSS files only','HTML style tags','Sass compiler only',0),
('react_native',4,'MEDIUM','What is React Navigation commonly used for?','Moving between application screens','Compiling native code','Storing database rows','Creating app icons',0),
('react_native',5,'HARD','Why use FlatList for a long collection?','It renders list items lazily','It disables scrolling','It converts data to SQL','It loads every item twice',0),

-- Swift and iOS
('swift_ios',1,'EASY','Which Swift keyword declares a constant?','var','let','const','final',1),
('swift_ios',2,'EASY','Which SwiftUI protocol describes a user-interface view?','View','Screen','Widget','Drawable',0),
('swift_ios',3,'MEDIUM','What does an Optional represent in Swift?','A value that may be absent','A global constant','A required protocol','A database query',0),
('swift_ios',4,'MEDIUM','Which syntax safely unwraps an optional for a conditional scope?','if let','try force','switch only','guard false only',0),
('swift_ios',5,'HARD','What does @State provide in SwiftUI?','View-owned mutable state that triggers updates','A shared SQL database','A network-only cache','An immutable environment value',0),

-- Machine learning
('machine_learning',1,'EASY','Which learning type uses labeled training examples?','Supervised learning','Unsupervised learning','Reinforcement only','Random search',0),
('machine_learning',2,'EASY','Which task predicts a continuous numeric value?','Classification','Regression','Clustering','Association rules',1),
('machine_learning',3,'MEDIUM','Why split data into training and test sets?','To estimate performance on unseen data','To duplicate every sample','To remove all features','To guarantee perfect accuracy',0),
('machine_learning',4,'MEDIUM','What is overfitting?','Learning training noise and generalizing poorly','Using too little memory','Reducing every feature','Choosing a linear model',0),
('machine_learning',5,'HARD','Which technique helps prevent data leakage during cross-validation?','Fit preprocessing within each fold','Normalize before any split','Use the test labels for tuning','Copy test rows into training',0),

-- Deep learning
('deep_learning',1,'EASY','What does a neuron activation function introduce?','Non-linearity','Database normalization','Network encryption','File compression',0),
('deep_learning',2,'EASY','Which network is especially suited to image feature extraction?','CNN','RDBMS','B-tree','REST',0),
('deep_learning',3,'MEDIUM','What does backpropagation compute?','Gradients of the loss','Database keys','Image file sizes','HTTP routes',0),
('deep_learning',4,'MEDIUM','Why use a validation set during training?','Tune choices without using the final test set','Increase label leakage','Replace training data','Guarantee zero loss',0),
('deep_learning',5,'HARD','What problem can ReLU help reduce compared with sigmoid in deep networks?','Vanishing gradients in positive regions','Class imbalance only','Missing labels','Data serialization',0),

-- Artificial intelligence
('ai',1,'EASY','Which search algorithm expands the shallowest nodes first?','Depth-first search','Breadth-first search','Hill climbing only','Backtracking only',1),
('ai',2,'EASY','What does NLP focus on?','Human language processing','Network packet routing','Database indexing','Image compression only',0),
('ai',3,'MEDIUM','What is a heuristic in informed search?','An estimate of cost to a goal','A guaranteed final answer','A training label','A database constraint',0),
('ai',4,'MEDIUM','Which computer vision task assigns a class to an entire image?','Image classification','Tokenization','Speech synthesis','Path planning',0),
('ai',5,'HARD','Why is A* optimal with an admissible heuristic under standard conditions?','The heuristic never overestimates remaining cost','It ignores path cost','It searches randomly','It expands only one node',0),

-- Pandas
('pandas',1,'EASY','What is a Pandas DataFrame?','A two-dimensional labeled table','A single Python integer','A chart renderer only','A database server',0),
('pandas',2,'EASY','Which function commonly reads a CSV file?','pd.read_csv','pd.open_table_only','pd.load_text','pd.csv',0),
('pandas',3,'MEDIUM','Which method identifies missing values?','isna()','missingRows()','emptyOnly()','nullIndex()',0),
('pandas',4,'MEDIUM','What does groupby() enable?','Split-apply-combine analysis','HTML rendering only','File encryption','Network requests',0),
('pandas',5,'HARD','Why use loc rather than iloc?','loc selects by labels while iloc selects by positions','loc always copies all data','iloc uses column names only','They are identical',0),

-- Matplotlib
('matplotlib',1,'EASY','Which function commonly creates a line chart?','plt.plot','plt.tableOnly','plt.lineFile','plt.drawAxisOnly',0),
('matplotlib',2,'EASY','Which function labels the horizontal axis?','plt.xlabel','plt.ylabel','plt.legend','plt.titleOnly',0),
('matplotlib',3,'MEDIUM','What does plt.legend() display?','Labels for plotted series','A data table','Only grid lines','The file path',0),
('matplotlib',4,'MEDIUM','Which chart is usually appropriate for comparing category values?','Bar chart','Scatter plot only','Contour map','Histogram of dates only',0),
('matplotlib',5,'HARD','Why call tight_layout() before saving a figure?','To reduce clipping and adjust subplot spacing','To sort the data','To add missing labels automatically','To change numeric types',0),

-- Selenium
('selenium',1,'EASY','What does Selenium WebDriver control?','A web browser','A database engine','A container registry','A compiler',0),
('selenium',2,'EASY','Which locator is generally most direct when an element has a stable unique value?','By.id','By.tagName only','By.xpath with absolute path','By.color',0),
('selenium',3,'MEDIUM','Why use an explicit wait?','To wait for a specific condition','To pause every test for a fixed hour','To disable asynchronous pages','To skip assertions',0),
('selenium',4,'MEDIUM','What does the Page Object Model encapsulate?','Page locators and interactions','Database migrations','CI server settings','Browser installation files',0),
('selenium',5,'HARD','Why are absolute XPath locators often brittle?','Small DOM structure changes can break them','They cannot find elements','They only work in Firefox','They require a database',0),

-- JUnit and Mockito
('junit_mockito',1,'EASY','Which JUnit 5 annotation marks a test method?','@Check','@Test','@Run','@Verify',1),
('junit_mockito',2,'EASY','What is a Mockito mock?','A test double with configurable behavior','A production database','A JUnit engine','A Java compiler',0),
('junit_mockito',3,'MEDIUM','Which assertion checks that two values are equal?','assertEquals','verifyEqualOnly','expectSameValue','checkMatch',0),
('junit_mockito',4,'MEDIUM','What does verify(service).save(item) check?','That the interaction occurred','That a database committed','That item is immutable','That compilation succeeded',0),
('junit_mockito',5,'HARD','Why avoid mocking the class under test?','It bypasses the behavior the test should verify','Mockito forbids all classes','It creates integration tests','It always starts a server',0),

-- Cybersecurity
('cybersecurity',1,'EASY','Which CIA triad property protects data from unauthorized change?','Confidentiality','Integrity','Availability','Accounting',1),
('cybersecurity',2,'EASY','What does multi-factor authentication require?','Evidence from more than one factor type','Two passwords only','A public username','No identity proof',0),
('cybersecurity',3,'MEDIUM','Why are passwords stored with a slow salted hash?','To resist offline cracking and rainbow tables','To allow decryption','To shorten every password','To remove authentication',0),
('cybersecurity',4,'MEDIUM','What is phishing?','Social engineering that impersonates a trusted source','Database normalization','Network load balancing','File compression',0),
('cybersecurity',5,'HARD','What control most directly limits lateral movement after compromise?','Network segmentation and least privilege','Shared administrator accounts','Open internal firewalls','Disabled logging',0),

-- Ethical hacking
('ethical_hacking',1,'EASY','What must exist before an authorized penetration test begins?','Written scope and permission','A public exploit only','Anonymous access','No rules of engagement',0),
('ethical_hacking',2,'EASY','What is reconnaissance used for?','Gathering information about the target','Deleting evidence','Deploying production code','Changing payroll data',0),
('ethical_hacking',3,'MEDIUM','Which vulnerability lets untrusted input alter a database query?','SQL injection','CSRF token','TLS pinning','Rate limiting',0),
('ethical_hacking',4,'MEDIUM','What is responsible disclosure?','Privately reporting a flaw and allowing remediation time','Publishing credentials immediately','Keeping every finding secret forever','Testing outside scope',0),
('ethical_hacking',5,'HARD','Why is a proof of concept preferred over destructive exploitation?','It demonstrates impact while minimizing harm','It hides all evidence','It expands scope automatically','It removes authorization needs',0),

-- Linux
('linux',1,'EASY','Which command prints the current working directory?','pwd','cwd','where','path',0),
('linux',2,'EASY','Which command lists directory contents?','ls','show','dirlist','files',0),
('linux',3,'MEDIUM','What does chmod change?','File permission bits','The file owner only','The current directory','A process ID',0),
('linux',4,'MEDIUM','Which command searches text using a pattern?','grep','mkdir','touch','uname',0),
('linux',5,'HARD','What does permission mode 750 grant to others?','No permissions','Read only','Read and execute','Full control',0),

-- Networking
('networking',1,'EASY','Which OSI layer routes packets between networks?','Data link','Network','Transport','Session',1),
('networking',2,'EASY','Which protocol translates domain names to IP addresses?','DHCP','DNS','ARP only','SSH',1),
('networking',3,'MEDIUM','What is the purpose of a subnet mask?','Identify network and host portions of an IP address','Encrypt Ethernet frames','Assign MAC addresses','Choose an HTTP method',0),
('networking',4,'MEDIUM','Which device forwards frames using MAC addresses?','Router only','Switch','DNS server','Firewall rule',1),
('networking',5,'HARD','How many usable host addresses are normally available in an IPv4 /27 subnet?','14','30','32','62',1),

-- GraphQL
('graphql',1,'EASY','What does a GraphQL schema define?','Available types and operations','A CSS layout','A container image','A Git history',0),
('graphql',2,'EASY','Which operation reads data without intending to modify it?','query','mutation','subscription only','resolver',0),
('graphql',3,'MEDIUM','What is a resolver responsible for?','Producing the value for a field','Parsing CSS','Starting a database server','Building a mobile app',0),
('graphql',4,'MEDIUM','What problem does GraphQL field selection help reduce?','Over-fetching response data','TLS encryption','Database backups','Source conflicts',0),
('graphql',5,'HARD','What is the N+1 resolver problem?','Repeated per-item data fetches cause excessive calls','A schema has too few types','One query returns no fields','A mutation uses variables',0),

-- WebSocket
('websocket',1,'EASY','What does WebSocket provide after its handshake?','Full-duplex communication','One static file only','A database transaction','Email delivery',0),
('websocket',2,'EASY','Which protocol usually initiates the WebSocket upgrade handshake?','HTTP','FTP','SMTP','DNS',0),
('websocket',3,'MEDIUM','Why are WebSockets useful for chat applications?','Servers can push messages immediately','They require page reloads','They store all messages automatically','They remove authentication needs',0),
('websocket',4,'MEDIUM','What should a client commonly do after an unexpected disconnect?','Reconnect with bounded backoff','Reconnect in a tight infinite loop','Delete its identity','Ignore every future message',0),
('websocket',5,'HARD','Why are sticky sessions or a shared broker needed when scaling stateful socket servers?','Related connections and messages must reach coordinated instances','WebSockets use SQL joins','Browsers support one server only','TLS prevents load balancing',0),

-- Agile and Scrum
('agile_scrum',1,'EASY','Who is accountable for maximizing product value in Scrum?','Product Owner','Scrum Master only','Developers manager','Stakeholder committee',0),
('agile_scrum',2,'EASY','What is a Sprint?','A fixed-length event for creating value','An unlimited project phase','A daily status report','A release branch only',0),
('agile_scrum',3,'MEDIUM','What is the purpose of the Daily Scrum?','Inspect progress toward the Sprint Goal and adapt the plan','Report to a manager only','Approve annual budgets','Write every requirement',0),
('agile_scrum',4,'MEDIUM','What does a Kanban work-in-progress limit encourage?','Finishing work before starting more','Larger batches','More simultaneous tasks','No workflow visibility',0),
('agile_scrum',5,'HARD','When should the Definition of Done be applied?','To every Increment','Only at final release','Only to documentation','After customer payment',0),

-- Software architecture
('architecture',1,'EASY','What does the layered architecture pattern separate?','Responsibilities into layers','All data into one variable','Every service into one process','UI colors by user',0),
('architecture',2,'EASY','What does MVC separate from business and data concerns?','Presentation concerns','Network cables','Git branches','Cloud regions',0),
('architecture',3,'MEDIUM','What is CQRS?','Separating command and query models','Caching every request','One database table per class','A UI component library',0),
('architecture',4,'MEDIUM','What does event sourcing store as the source of truth?','A sequence of domain events','Only current row values','Rendered HTML','Container logs only',0),
('architecture',5,'HARD','Which tradeoff often accompanies asynchronous event-driven systems?','Eventual consistency','Guaranteed global ordering everywhere','No observability needs','No failure handling',0),

-- Blockchain and Web3
('blockchain',1,'EASY','What is a blockchain block commonly linked to?','The hash of a previous block','A CSS selector','A DNS alias','A SQL view',0),
('blockchain',2,'EASY','What is a smart contract?','Program logic executed on a blockchain','A paper-only agreement','A database backup','A browser extension only',0),
('blockchain',3,'MEDIUM','What does a digital signature prove when verified?','Control of a private key and message integrity','That data is secret','That a transaction is free','That a block cannot exist',0),
('blockchain',4,'MEDIUM','Why must a private key remain secret?','It can authorize actions for its address','It stores the entire chain','It is a public identifier','It compresses blocks',0),
('blockchain',5,'HARD','What is a reentrancy vulnerability in a smart contract?','External code calls back before state is safely updated','A block has two hashes','A wallet uses MFA','A node reconnects to peers',0);

-- Questions 6-10 for every quiz. These extend each bank to ten questions.
INSERT INTO _quiz_questions
    (seed_key, question_order, difficulty, question_text,
     option_a, option_b, option_c, option_d, correct_index)
VALUES
-- HTML checkpoint
('html_mid',6,'EASY','Which declaration tells a browser that a document uses HTML5?','<!DOCTYPE html>','<html version="5">','<meta html="5">','<doctype>HTML5</doctype>',0),
('html_mid',7,'EASY','Which attribute of an anchor contains its destination URL?','src','href','action','targetUrl',1),
('html_mid',8,'MEDIUM','Which semantic element should contain the dominant content unique to a page?','<aside>','<footer>','<main>','<nav>',2),
('html_mid',9,'MEDIUM','Which input attribute prevents an empty form control from passing browser validation?','validate','mandatory','required','notempty',2),
('html_mid',10,'HARD','How is a label explicitly associated with an input whose id is email?','<label input="email">','<label for="email">','<label href="email">','<label name="email">',1),

-- HTML and CSS final
('html_final',6,'EASY','Which CSS property changes text color?','font-color','text-color','color','foreground',2),
('html_final',7,'EASY','Which unit is relative to the root element font size?','px','rem','vh','cm',1),
('html_final',8,'MEDIUM','Which selector has the highest specificity?','p','.note','#notice','*',2),
('html_final',9,'MEDIUM','Which declaration creates a three-column grid with equal tracks?','grid-columns: 3','grid-template-columns: repeat(3, 1fr)','columns: 1fr 1fr 1fr','grid: columns(3)',1),
('html_final',10,'HARD','What does box-sizing: border-box include in the declared width?','Margin only','Content only','Content, padding, and border','Padding and margin only',2),

-- JavaScript
('javascript',6,'EASY','Which operator returns the remainder after division?','/','%','//','**',1),
('javascript',7,'EASY','Which method keeps array elements that pass a test?','map','filter','reduceOnly','concat',1),
('javascript',8,'MEDIUM','What is a closure?','A function bundled with access to its lexical scope','A closed browser tab','A frozen array only','A rejected Promise',0),
('javascript',9,'MEDIUM','What does event bubbling describe?','An event propagating from a target toward ancestors','A timer repeating forever','A Promise resolving twice','An array being sorted',0),
('javascript',10,'HARD','Which call safely queues a state-independent task after the current synchronous stack?','setTimeout(task, 0)','task.awaitNow()','sleep(task)','queueBlock(task)',0),

-- React
('react',6,'EASY','Which prop names a click handler in React JSX?','onclick','onClick','clickHandlerOnly','on-click',1),
('react',7,'EASY','Which value can a component return to render nothing?','void','null','0 only','undefined only',1),
('react',8,'MEDIUM','What makes an input a controlled React component?','Its value is driven by state and updated by an event handler','It has a CSS class','It uses a ref only','It has no value prop',0),
('react',9,'MEDIUM','What does an empty dependency array usually mean for an effect?','Run after every render','Run after initial mount, then clean up on unmount','Never run','Run before JSX evaluation',1),
('react',10,'HARD','Why use setCount(current => current + 1) for an update based on previous state?','It receives the latest queued state value','It mutates props','It disables batching','It prevents rendering',0),

-- Vue
('vue',6,'EASY','Which directive repeats markup for items in a collection?','v-loop','v-for','v-each-only','v-repeat',1),
('vue',7,'EASY','How does a child component normally receive data from its parent?','Props','Global DOM variables','Slots only','Lifecycle hooks',0),
('vue',8,'MEDIUM','Why should v-for items use a stable :key?','To preserve element identity during updates','To add an event listener','To make the array immutable','To create a route',0),
('vue',9,'MEDIUM','How should a child normally notify its parent of an event?','Mutate the parent directly','Emit a declared event','Edit a global variable','Reload the page',1),
('vue',10,'HARD','When is watch preferable to computed?','For performing side effects when reactive data changes','For every cached derived value','For static markup only','For declaring props',0),

-- Angular
('angular',6,'EASY','Which syntax displays a component value as text in a template?','[[ value ]]','{{ value }}','( value )','## value',1),
('angular',7,'EASY','Which decorator marks data received from a parent component?','@Output','@Input','@Injectable','@Host',1),
('angular',8,'MEDIUM','What does an @Output property commonly expose?','An EventEmitter','A database connection','A CSS selector','A route guard only',0),
('angular',9,'MEDIUM','What is Angular Router used for?','Mapping URLs to application views','Compiling TypeScript types','Creating database rows','Managing CSS variables',0),
('angular',10,'HARD','What is a benefit of OnPush change detection?','It can reduce unnecessary component checks','It disables input bindings','It makes all objects mutable','It removes dependency injection',0),

-- TypeScript
('typescript',6,'EASY','Which type permits either a string or a number?','string & number','string | number','string + number','either<string, number>',1),
('typescript',7,'EASY','Which syntax declares a tuple containing a string then a number?','[string, number]','string[number]','Array<string | number> only','(string, number)',0),
('typescript',8,'MEDIUM','Why is unknown safer than any?','It requires narrowing before most operations','It accepts no values','It exists only at runtime','It is always a string',0),
('typescript',9,'MEDIUM','What does keyof T produce?','A union of known property keys of T','All property values','A runtime array','A new class instance',0),
('typescript',10,'HARD','Which check narrows value: string | number to string?','value === String','typeof value === "string"','value.type === "string"','String(value) only',1),

-- Java
('java',6,'EASY','What executes Java bytecode?','JVM','JDK compiler only','BIOS','Node.js',0),
('java',7,'EASY','What is a constructor used for?','Initialize a new object','Destroy a class','Import a package','Override an interface',0),
('java',8,'MEDIUM','What can a Java class do with multiple interfaces?','Implement them','Extend all of them as classes','Instantiate none of them','Import them only',0),
('java',9,'MEDIUM','What does final on a class prevent?','Instantiation','Inheritance','Method calls','Package access',1),
('java',10,'HARD','Why should equal objects normally have equal hashCode values?','Hash-based collections depend on that contract','The compiler requires unique hashes','It makes objects immutable','It prevents garbage collection',0),

-- Spring Boot
('spring_boot',6,'EASY','Which annotation combines configuration, auto-configuration, and component scanning?','@SpringBootApplication','@EnableWebOnly','@BootController','@ApplicationBean',0),
('spring_boot',7,'EASY','Which annotation binds a JSON request body to a method parameter?','@RequestBody','@RequestParamOnly','@ModelTable','@JsonRoute',0),
('spring_boot',8,'MEDIUM','Why return ResponseEntity from a controller?','To control status, headers, and body','To open a JPA transaction only','To scan components','To create an entity',0),
('spring_boot',9,'MEDIUM','What are Spring profiles useful for?','Activating environment-specific configuration','Defining SQL primary keys','Encoding JWTs only','Replacing dependency injection',0),
('spring_boot',10,'HARD','What commonly causes the JPA N+1 query problem?','Lazy relationships fetched once per parent row','One bulk query','A database index','A read-only transaction',0),

-- C#
('csharp',6,'EASY','Which directive imports types from a namespace?','include','using','import','namespaceRef',1),
('csharp',7,'EASY','Which generic collection is a resizable ordered sequence?','List<T>','HashSet<T>','Dictionary<T,T> only','Queue<T> only',0),
('csharp',8,'MEDIUM','Which LINQ method filters a sequence by a predicate?','Select','Where','OrderByOnly','Aggregate',1),
('csharp',9,'MEDIUM','What return type commonly represents an asynchronous operation with no result value?','Task','void only','Thread','Promise<T>',0),
('csharp',10,'HARD','What does a using statement do for an IDisposable object?','Ensures Dispose is called','Makes it global','Serializes it to JSON','Runs it on another process',0),

-- ASP.NET Core
('aspnet',6,'EASY','Where is the request pipeline commonly configured in modern ASP.NET Core?','Program.cs','model.json','Views.config','database.cs only',0),
('aspnet',7,'EASY','Which result helper returns HTTP 404 from a controller?','Missing()','NotFound()','NoRoute()','Empty()',1),
('aspnet',8,'MEDIUM','What is model binding?','Mapping request data to action parameters and models','Generating database indexes','Bundling CSS files','Compiling Razor to SQL',0),
('aspnet',9,'MEDIUM','What does app.UseAuthentication() establish?','The user identity for later authorization','A database transaction','A response cache only','A static file directory',0),
('aspnet',10,'HARD','Why should authentication middleware run before authorization middleware?','Authorization needs the established user identity','Authentication writes the response body','Authorization creates controllers','Order never matters',0),

-- Python
('python',6,'EASY','Which collection stores key-value pairs?','list','dict','tuple','set only',1),
('python',7,'EASY','Which keyword loads a module?','include','import','using','require',1),
('python',8,'MEDIUM','What does yield make a function return?','A generator','A class','A module','A dictionary only',0),
('python',9,'MEDIUM','Why is a tuple useful for fixed data?','It is immutable','It always sorts itself','It permits duplicate keys','It is a database table',0),
('python',10,'HARD','Why avoid a mutable list as a default parameter value?','The same list is reused across calls','Lists cannot be parameters','It causes a syntax error','The list becomes a tuple',0),

-- Django
('django',6,'EASY','Which layer contains presentation templates in Django MTV?','Template','Model','View only','Migration',0),
('django',7,'EASY','Which shortcut combines a template with context into an HTTP response?','render()','compose()','templateOnly()','respondFile()',0),
('django',8,'MEDIUM','What does a ForeignKey model field represent?','A many-to-one relationship','A CSS import','A URL namespace','A static file',0),
('django',9,'MEDIUM','When is select_related() useful?','Following single-valued relationships in one SQL join','Uploading media files','Rendering forms only','Creating migrations',0),
('django',10,'HARD','Why wrap related writes in transaction.atomic()?','They commit or roll back as one unit','They become read-only','They skip validation','They run in the browser',0),

-- Node.js and Express
('node',6,'EASY','What lets Node.js handle many I/O operations without one thread per request?','The event loop','The browser DOM','A SQL trigger','CSS modules',0),
('node',7,'EASY','Which middleware parses JSON request bodies in Express?','express.json()','express.bodyText()','app.parseJsonOnly()','JSON.middleware()',0),
('node',8,'MEDIUM','Where does Express place a route value from /users/:id?','req.params.id','req.body.route','res.params.id','req.headers.routeId',0),
('node',9,'MEDIUM','What does module.exports define in CommonJS?','Values exposed by a module','Environment variables','HTTP headers','Database indexes',0),
('node',10,'HARD','Why respect stream backpressure?','To avoid producing data faster than it can be consumed','To force synchronous I/O','To disable buffers','To close every socket',0),

-- REST API
('rest_api',6,'EASY','How should REST resource paths usually be named?','With resource nouns','With UI button labels','With SQL statements','With file extensions only',0),
('rest_api',7,'EASY','Which status code means authentication credentials are required or invalid?','401','403','404','409',0),
('rest_api',8,'MEDIUM','Which method usually replaces the full representation at a known URI?','PATCH','PUT','GET','OPTIONS',1),
('rest_api',9,'MEDIUM','What can an ETag support?','Conditional requests and cache validation','Password hashing','Database migration','DNS resolution',0),
('rest_api',10,'HARD','What is a benefit of cursor pagination over large offsets?','Stable efficient traversal of changing large datasets','It returns every row','It needs no ordering','It disables caching',0),

-- Microservices
('microservices',6,'EASY','What communication style uses events without waiting for an immediate response?','Asynchronous messaging','Synchronous RPC only','Shared memory only','Batch SQL only',0),
('microservices',7,'EASY','What does centralized tracing help follow?','A request across multiple services','Only local file names','A CSS cascade','A database password',0),
('microservices',8,'MEDIUM','What does the bulkhead pattern isolate?','Failures and resource exhaustion','All source repositories','Every API route in one pool','Database schemas only',0),
('microservices',9,'MEDIUM','What does a Saga coordinate?','A distributed business transaction using local steps','One global database lock','A UI animation','A container build only',0),
('microservices',10,'HARD','Why attach a correlation ID to messages and requests?','To connect distributed logs and traces','To encrypt payloads','To choose a database index','To allocate CPU cores',0),

-- Data structures and algorithms
('dsa',6,'EASY','Which data structure follows first in, first out?','Stack','Queue','Set','Tree',1),
('dsa',7,'EASY','Which structure efficiently returns the current minimum or maximum priority item?','Heap','Linked list only','Hash collision','Adjacency matrix only',0),
('dsa',8,'MEDIUM','What makes a sorting algorithm stable?','Equal keys retain their relative order','It always runs in O(1)','It uses no memory','It only sorts numbers',0),
('dsa',9,'MEDIUM','Which structure is used by an iterative depth-first graph traversal?','Queue','Stack','Hash value only','Priority counter',1),
('dsa',10,'HARD','Which condition is required for standard Dijkstra shortest-path correctness?','Non-negative edge weights','A tree with no branches','Negative cycles only','All weights equal zero',0),

-- SQL and MySQL
('sql_mysql',6,'EASY','Which keyword removes duplicate rows from a SELECT result?','UNIQUE','DISTINCT','DEDUP','GROUPONLY',1),
('sql_mysql',7,'EASY','Which command permanently saves the current transaction changes?','SAVE','COMMIT','APPLY','MERGE',1),
('sql_mysql',8,'MEDIUM','What does a LEFT JOIN preserve?','Every row from the left table','Only matching rows','Every row from the right table only','No unmatched rows',0),
('sql_mysql',9,'MEDIUM','What is a composite index?','An index over multiple columns','Two identical tables','An encrypted primary key','A stored procedure',0),
('sql_mysql',10,'HARD','Why may an index on (customer_id, created_at) not efficiently serve a filter on created_at alone?','It does not use the leftmost indexed column','Dates cannot be indexed','Composite indexes support equality only','The index is always unique',0),

-- PostgreSQL
('postgresql',6,'EASY','Which command refreshes planner statistics for tables?','ANALYZE','REINDEX ONLY','PLAN CACHE','DESCRIBE',0),
('postgresql',7,'EASY','What does WAL stand for in PostgreSQL?','Write-Ahead Logging','Wide Access Layer','Worker Allocation List','Write After Lock',0),
('postgresql',8,'MEDIUM','What is a PostgreSQL role used for?','Authentication and authorization','Defining a table row only','Compressing backups','Rendering JSON',0),
('postgresql',9,'MEDIUM','What advantage does JSONB have over plain JSON for querying?','Parsed binary storage with indexing support','It preserves whitespace exactly','It forbids nested data','It is always smaller',0),
('postgresql',10,'HARD','What does a partial index contain?','Rows satisfying its WHERE predicate','Only part of each column value','Every second table page','Uncommitted rows only',0),

-- MongoDB
('mongodb',6,'EASY','Which method inserts one document?','insertOne()','addRow()','createRecord()','appendDocOnly()',0),
('mongodb',7,'EASY','Which field is the default unique identifier for a document?','id','_id','documentKeyOnly','uuid_field',1),
('mongodb',8,'MEDIUM','Which update operator changes selected fields without replacing the document?','$set','$match','$group','$project',0),
('mongodb',9,'MEDIUM','What does a query projection control?','Which fields are returned','Which server accepts writes','How indexes are built','When backups run',0),
('mongodb',10,'HARD','Why does field order matter in a compound index?','It affects which query prefixes can use the index efficiently','MongoDB sorts field names alphabetically','Only the last field is indexed','It changes document validation',0),

-- Redis
('redis',6,'EASY','Which command stores a string value at a key?','PUT','SET','WRITE','ADDVALUE',1),
('redis',7,'EASY','Which command removes a key?','DROP','DEL','REMOVEKEY','UNSET',1),
('redis',8,'MEDIUM','Which pair starts and executes a Redis transaction block?','BEGIN and COMMIT','MULTI and EXEC','START and RUN','WATCH and GET',1),
('redis',9,'MEDIUM','What does an eviction policy decide?','Which keys to remove when memory is constrained','Which users may connect','How commands are encrypted','How channels are named',0),
('redis',10,'HARD','What is the purpose of WATCH in optimistic transactions?','Abort EXEC if watched keys changed','Persist every command to disk','Lock the whole server','Subscribe to a channel',0),

-- Docker
('docker',6,'EASY','What is a running instance of an image called?','Layer','Container','Registry','Volume',1),
('docker',7,'EASY','Which option publishes a container port to the host?','-p','-v','-e','-d only',0),
('docker',8,'MEDIUM','What is the purpose of .dockerignore?','Exclude files from the build context','Ignore running containers','Disable image layers','Skip all build errors',0),
('docker',9,'MEDIUM','What does CMD define in a Dockerfile?','The default container command','A build-stage name','A volume driver','A registry password',0),
('docker',10,'HARD','Why copy dependency manifests before application source in a Dockerfile?','To reuse cached dependency layers when source changes','To publish more ports','To disable build cache','To merge all layers',0),

-- Kubernetes
('kubernetes',6,'EASY','Which command declaratively creates or updates resources from a manifest?','kubectl apply','kubectl compile','kube runfile','cluster deploy-only',0),
('kubernetes',7,'EASY','Which object stores non-secret configuration values?','ConfigMap','Deployment','ReplicaSet','Ingress',0),
('kubernetes',8,'MEDIUM','What do container resource requests influence?','Pod scheduling','DNS record names only','Image tags','Service selectors only',0),
('kubernetes',9,'MEDIUM','What does a HorizontalPodAutoscaler adjust?','Replica count based on observed metrics','Container image contents','Cluster certificates only','Namespace names',0),
('kubernetes',10,'HARD','Why is base64 data in a Kubernetes Secret not sufficient encryption by itself?','Base64 is reversible encoding','Secrets cannot hold strings','Pods cannot read Secrets','TLS disables base64',0),

-- AWS
('aws',6,'EASY','Which AWS service runs functions in response to events without server management?','Lambda','EBS','VPC','IAM',0),
('aws',7,'EASY','Which service collects AWS metrics and logs?','CloudWatch','CloudFormation only','CodeCommit','Direct Connect',0),
('aws',8,'MEDIUM','What does the shared responsibility model mean?','AWS and the customer secure different layers','AWS secures all customer data automatically','Customers maintain AWS datacenters','No one manages identity',0),
('aws',9,'MEDIUM','What does a VPC provide?','A logically isolated virtual network','A global object bucket','A managed source repository','A billing invoice only',0),
('aws',10,'HARD','What is true of an EC2 security group?','It is stateful','It is a stateless subnet ACL','It runs SQL queries','It stores encryption keys',0),

-- Azure
('azure',6,'EASY','Which Azure service runs event-driven serverless functions?','Azure Functions','Virtual Network','Load Balancer only','Storage Explorer',0),
('azure',7,'EASY','Which service provides managed SQL Server databases?','Azure SQL Database','Azure DNS','Event Grid','Container Registry',0),
('azure',8,'MEDIUM','What does Azure RBAC control?','Who can perform actions on resources','How SQL rows are sorted','Which region is cheapest','How blobs are compressed',0),
('azure',9,'MEDIUM','Why use a managed identity for an Azure resource?','Access services without storing application credentials','Give every user owner access','Create a public IP automatically','Disable token authentication',0),
('azure',10,'HARD','At what scope can an Azure role assignment be inherited by child resources?','Management group, subscription, or resource group','Only one virtual machine file','Only a browser session','Only an SQL table',0),

-- Google Cloud
('gcp',6,'EASY','Which service provides an analytics data warehouse?','BigQuery','Cloud DNS','Artifact Registry only','Cloud VPN',0),
('gcp',7,'EASY','Which service provides asynchronous message topics and subscriptions?','Pub/Sub','Cloud SQL','Compute Engine','Cloud Armor',0),
('gcp',8,'MEDIUM','What is a service account intended to identify?','An application or workload','A billing currency','A human team only','A storage object',0),
('gcp',9,'MEDIUM','What does Cloud Run deploy?','Stateless containers on managed infrastructure','Raw physical disks only','Desktop applications','DNS root servers',0),
('gcp',10,'HARD','Why prefer predefined or custom least-privilege IAM roles over basic roles?','They grant more precise permissions','They disable audit logs','They make resources public','They require no identity',0),

-- Git and GitHub
('git_github',6,'EASY','Which command shows modified, staged, and untracked files?','git status','git inspect','git changes-only','git files',0),
('git_github',7,'EASY','Which command records staged changes in local history?','git commit','git fetch','git pull','git diff',0),
('git_github',8,'MEDIUM','What does git fetch do?','Downloads remote refs without merging them','Deletes local branches','Commits staged files','Uploads local commits',0),
('git_github',9,'MEDIUM','What is a merge conflict?','Git cannot automatically combine overlapping changes','A remote has no README','A commit has one parent','A repository is private',0),
('git_github',10,'HARD','Why is git revert generally safer than rewriting a shared branch?','It adds an inverse commit without changing published history','It deletes all previous commits','It bypasses code review','It removes the remote',0),

-- CI/CD
('cicd',6,'EASY','What is a pipeline artifact?','A produced file retained for later stages or delivery','A Git conflict','A running database only','A user password',0),
('cicd',7,'EASY','How should deployment credentials be supplied to a workflow?','Through protected secret storage','Committed in source code','Printed in logs','Placed in public artifacts',0),
('cicd',8,'MEDIUM','What is a canary deployment?','Releasing to a small subset before wider rollout','Deploying every version at once','Testing only on a laptop','Deleting the prior release first',0),
('cicd',9,'MEDIUM','What is the difference between a cache and an artifact?','A cache speeds future jobs; an artifact is an intended output','They are always identical','Artifacts cannot be downloaded','Caches are production releases',0),
('cicd',10,'HARD','What metric should trigger an automated canary rollback?','A defined regression such as elevated error rate','A successful health check','An unchanged commit hash','A smaller image',0),

-- Terraform
('terraform',6,'EASY','Which command initializes providers and modules?','terraform init','terraform start','terraform provider-run','terraform setup-state-only',0),
('terraform',7,'EASY','Which command executes an approved plan?','terraform apply','terraform commit','terraform release','terraform run-plan-only',0),
('terraform',8,'MEDIUM','What is a Terraform provider?','A plugin that manages a platform API','A state backup only','An HCL comment','A Git remote',0),
('terraform',9,'MEDIUM','What is infrastructure drift?','Real infrastructure differs from configuration or state expectations','A module has variables','A plan has no changes','A provider is downloaded',0),
('terraform',10,'HARD','When is explicit depends_on useful?','A dependency exists but cannot be inferred from references','Every resource needs it','To encrypt state','To select a workspace only',0),

-- UI and UX
('uiux',6,'EASY','What is an affordance in interface design?','A clue about how an element can be used','A database permission','A font file only','A network request',0),
('uiux',7,'EASY','What is a prototype used to explore?','Interactions and flows before full implementation','Only final source code','Production database capacity','Server certificates',0),
('uiux',8,'MEDIUM','What should a useful persona be based on?','Research evidence about target users','A designer stereotype','One random color palette','Competitor source code',0),
('uiux',9,'MEDIUM','Why use consistent interaction patterns?','Users can transfer learned expectations','Every screen becomes identical','It removes accessibility needs','It prevents iteration',0),
('uiux',10,'HARD','Which change most directly lowers cognitive load in a complex form?','Group related fields and reveal detail progressively','Show all advanced options first','Remove every label','Use many unrelated colors',0),

-- Figma
('figma',6,'EASY','What do constraints control inside a resized frame?','How a layer is positioned and sized','Who can edit the file','Which font is installed','How SQL is generated',0),
('figma',7,'EASY','What does a prototype connection define?','An interaction between frames','A component property only','A database relation','A file export size',0),
('figma',8,'MEDIUM','Why publish components to a team library?','Make shared design assets available across files','Flatten all components','Disable updates','Convert them to screenshots',0),
('figma',9,'MEDIUM','What are variables useful for in a design system?','Reusable values and modes such as color tokens','Running JavaScript servers','Creating Git commits','Compressing images only',0),
('figma',10,'HARD','What is a consequence of detaching a component instance?','It stops receiving main component updates','It deletes the main component','It publishes a library','It creates Auto Layout',0),

-- Android and Kotlin
('android_kotlin',6,'EASY','Which Activity callback is commonly used for initial setup?','onCreate','onDestroyOnly','onPause','onStop',0),
('android_kotlin',7,'EASY','Which Android library provides an abstraction over SQLite?','Room','Retrofit only','Glide','JUnit',0),
('android_kotlin',8,'MEDIUM','Why avoid long-running work on the Android main thread?','It can freeze the UI and cause ANRs','It improves rendering too much','It disables storage','It changes the app package',0),
('android_kotlin',9,'MEDIUM','Which coroutine dispatcher is suited to blocking I/O work?','Dispatchers.IO','Dispatchers.Main','Dispatchers.Unconfined only','No dispatcher',0),
('android_kotlin',10,'HARD','What does the navigation back stack represent?','Destinations the user can return through','Downloaded APK files','Database migrations only','RecyclerView rows',0),

-- Flutter
('flutter',6,'EASY','What does hot reload primarily preserve while updating code?','Current application state','The release signing key','Database backups','App store metadata',0),
('flutter',7,'EASY','Which widget commonly provides app-level Material configuration?','MaterialApp','Container','Text','SafeArea only',0),
('flutter',8,'MEDIUM','What does Navigator.push add?','A route to the navigation stack','A widget to a database','A package to pubspec','A new isolate only',0),
('flutter',9,'MEDIUM','What does a Future represent in Dart?','A value or error available later','A synchronous list only','A widget key','A database table',0),
('flutter',10,'HARD','Why give stateful list items stable Keys?','To preserve widget identity when order changes','To set text color','To create routes','To enable hot reload only',0),

-- React Native
('react_native',6,'EASY','Which component handles configurable press interactions?','Pressable','Anchor','ButtonHTML','ClickViewOnly',0),
('react_native',7,'EASY','What is the default flexDirection in React Native?','row','column','grid','inline',1),
('react_native',8,'MEDIUM','What does the Platform API help implement?','Platform-specific behavior','Database schemas','React state only','Network encryption',0),
('react_native',9,'MEDIUM','When is a native module needed?','To expose unavailable platform APIs to JavaScript','For every Text component','To define props','To create a FlatList',0),
('react_native',10,'HARD','Why remove event listeners in an effect cleanup?','To prevent leaks and duplicate handlers','To disable navigation','To make props mutable','To force native compilation',0),

-- Swift and iOS
('swift_ios',6,'EASY','Which syntax declares a mutable Swift variable?','let score','var score','mutable score','value score',1),
('swift_ios',7,'EASY','Which collection stores an ordered sequence in Swift?','Array','Set','Dictionary only','Protocol',0),
('swift_ios',8,'MEDIUM','What is guard commonly used for?','Early exit when required conditions fail','Declaring stored properties','Creating animations only','Importing modules',0),
('swift_ios',9,'MEDIUM','What does a Swift protocol define?','A blueprint of required capabilities','A concrete database row','A memory address only','A package version',0),
('swift_ios',10,'HARD','Why is a delegate reference often declared weak?','To avoid a strong reference cycle','To make calls synchronous','To copy the delegate','To prevent protocol conformance',0),

-- Machine learning
('machine_learning',6,'EASY','What is a feature in a supervised learning dataset?','An input variable','The predicted model file','A test score only','A database key',0),
('machine_learning',7,'EASY','What is a class label?','A target category','A numeric feature only','A missing value','A model parameter',0),
('machine_learning',8,'MEDIUM','Why standardize features for distance-based models?','Prevent large-scale features from dominating distances','Create more training rows','Remove every outlier','Convert labels to text',0),
('machine_learning',9,'MEDIUM','What does a confusion matrix summarize?','Predicted versus actual classes','Only training duration','Feature correlations only','Model file size',0),
('machine_learning',10,'HARD','Why tune hyperparameters with cross-validation instead of the test set?','To preserve an unbiased final evaluation','To guarantee perfect predictions','To eliminate training','To increase data leakage',0),

-- Deep learning
('deep_learning',6,'EASY','What is an epoch?','One pass through the training dataset','One neuron only','A model file extension','A test label',0),
('deep_learning',7,'EASY','What does an optimizer update during training?','Model parameters','Input labels','Image dimensions','Database records',0),
('deep_learning',8,'MEDIUM','What is dropout intended to reduce?','Overfitting','Batch size','Class count','Input resolution only',0),
('deep_learning',9,'MEDIUM','What is transfer learning?','Adapting a pretrained model to a new task','Copying test labels into training','Training without parameters','Replacing gradients with SQL',0),
('deep_learning',10,'HARD','Why pair softmax output with cross-entropy for single-label classification?','It models class probabilities and penalizes incorrect likelihood','It removes all nonlinearities','It guarantees balanced data','It creates convolution filters',0),

-- Artificial intelligence
('ai',6,'EASY','What is an intelligent agent?','A system that perceives and acts in an environment','A database index','A static image','A network cable',0),
('ai',7,'EASY','Which learning setup receives rewards from interactions?','Reinforcement learning','Clustering only','Static compilation','Database replication',0),
('ai',8,'MEDIUM','Which algorithm is commonly used for adversarial game search?','Minimax','K-means','Dijkstra only','Linear regression',0),
('ai',9,'MEDIUM','What does recall measure in binary classification?','The fraction of actual positives found','The fraction of predictions that are negative','Overall file size','Training speed',0),
('ai',10,'HARD','Why audit an AI dataset for representation bias?','Skewed data can produce systematically unfair outcomes','Bias always improves accuracy','Models ignore training data','It reduces storage only',0),

-- Pandas
('pandas',6,'EASY','What is a Pandas Series?','A one-dimensional labeled array','A two-dimensional chart','A SQL server','A Python module loader',0),
('pandas',7,'EASY','Which method previews the first rows of a DataFrame?','head()','firstRowsOnly()','peekFile()','topData()',0),
('pandas',8,'MEDIUM','Which method replaces missing values with a supplied value?','fillna()','replaceNullRowsOnly()','complete()','notna()',0),
('pandas',9,'MEDIUM','Which operation combines DataFrames using matching keys?','merge()','plot()','describe()','astype()',0),
('pandas',10,'HARD','Why are vectorized Pandas operations usually preferred over Python row loops?','They use optimized array operations and are usually faster','They always use less memory','Loops cannot read values','They remove missing data automatically',0),

-- Matplotlib
('matplotlib',6,'EASY','What object is the overall Matplotlib drawing container?','Figure','Legend','Tick','Line only',0),
('matplotlib',7,'EASY','Which function creates a scatter plot?','plt.scatter','plt.pointsOnly','plt.cloud','plt.dotsFile',0),
('matplotlib',8,'MEDIUM','What does a histogram show?','The distribution of numeric values across bins','Relationships between two categories only','A geographic route','A database execution plan',0),
('matplotlib',9,'MEDIUM','What does plt.subplots() conveniently create?','A Figure and one or more Axes','Only a legend','A CSV file','A Seaborn theme only',0),
('matplotlib',10,'HARD','When is a logarithmic axis useful?','Values span several orders of magnitude','Every value is identical','Categories have no order','The plot contains text only',0),

-- Selenium
('selenium',6,'EASY','Which WebDriver method navigates to a URL?','get()','openTabOnly()','request()','browseToFile()',0),
('selenium',7,'EASY','What does findElement return when a match is found?','A WebElement','An HTTP response','A database row','A test report',0),
('selenium',8,'MEDIUM','Why avoid mixing large implicit waits with explicit waits?','Combined timing can become unpredictable','They disable locators','Browsers reject both APIs','They make tests unit tests',0),
('selenium',9,'MEDIUM','What causes a stale element reference?','The stored element no longer matches the current DOM','The URL uses HTTPS','A locator uses an id','The browser has cookies',0),
('selenium',10,'HARD','What is essential when running WebDriver tests in parallel?','Isolated driver instances and test data','One shared mutable driver','Fixed sleeps in every step','A single global account only',0),

-- JUnit and Mockito
('junit_mockito',6,'EASY','Which JUnit 5 annotation runs setup before each test?','@BeforeEach','@BeforeAllTestsOnly','@Setup','@Initialize',0),
('junit_mockito',7,'EASY','Which assertion verifies that code throws an expected exception?','assertThrows','assertErrorOnly','verifyException','expectFailure',0),
('junit_mockito',8,'MEDIUM','What does when(repo.find()).thenReturn(value) configure?','Stubbed mock behavior','A production transaction','A JUnit lifecycle','Java inheritance',0),
('junit_mockito',9,'MEDIUM','What is a Mockito spy?','A partial test double wrapping a real object','A completely unrelated interface','A database profiler','A JUnit report',0),
('junit_mockito',10,'HARD','When is an integration test more appropriate than a mocked unit test?','When verifying real component collaboration or infrastructure behavior','When testing a pure calculation only','When no dependencies exist','When execution must never start Spring',0),

-- Cybersecurity
('cybersecurity',6,'EASY','Which control primarily protects confidentiality of stored data?','Encryption at rest','A public checksum','Load balancing','A larger disk',0),
('cybersecurity',7,'EASY','Which practice most directly improves recovery from ransomware?','Tested offline or immutable backups','Shared passwords','Disabled updates','Open remote access',0),
('cybersecurity',8,'MEDIUM','Why is prompt security patching important?','It closes known exploitable vulnerabilities','It replaces access control','It guarantees no future bugs','It disables malware automatically',0),
('cybersecurity',9,'MEDIUM','What is a core Zero Trust principle?','Continuously verify explicitly and minimize access','Trust every internal device','Use one network perimeter only','Disable identity checks',0),
('cybersecurity',10,'HARD','What is the immediate goal of incident containment?','Limit further damage while preserving response options','Erase every log','Publish all evidence','Restore before understanding scope',0),

-- Ethical hacking
('ethical_hacking',6,'EASY','What does vulnerability scanning primarily identify?','Potential known weaknesses','Guaranteed exploit success','Employee salaries','Source licenses only',0),
('ethical_hacking',7,'EASY','What does CVSS communicate?','Standardized vulnerability severity characteristics','A password hash','A network route','A legal authorization',0),
('ethical_hacking',8,'MEDIUM','Which output context is directly at risk from reflected XSS?','Unescaped attacker-controlled browser HTML','A parameterized SQL query','An offline encrypted backup','A private network route',0),
('ethical_hacking',9,'MEDIUM','What does the HttpOnly cookie flag reduce?','JavaScript access to the cookie','TLS certificate validation','Server-side authorization','Password length',0),
('ethical_hacking',10,'HARD','What should a penetration tester do with test accounts and artifacts after completion?','Remove them as agreed and document cleanup','Leave persistent access','Hide them from the client','Expand their privileges',0),

-- Linux
('linux',6,'EASY','Which command changes the current directory?','cd','mv','pwd','dirset',0),
('linux',7,'EASY','Which command copies files?','cp','mv','touch','ln only',0),
('linux',8,'MEDIUM','Which command lists running processes in a snapshot?','ps','chmod','grep only','df',0),
('linux',9,'MEDIUM','What does systemctl manage on systemd-based systems?','Services and system units','SQL tables','User documents only','Shell variables',0),
('linux',10,'HARD','What does the pipe operator do in a shell command?','Sends one command output to another command input','Runs a command as root','Writes only to a file','Comments out a command',0),

-- Networking
('networking',6,'EASY','Which transport protocol provides reliable ordered delivery?','UDP','TCP','IP only','ARP',1),
('networking',7,'EASY','What is the default port for HTTPS?','22','53','80','443',3),
('networking',8,'MEDIUM','What does ARP resolve on an IPv4 local network?','An IP address to a MAC address','A domain to an IP address','A port to a process only','A route to a password',0),
('networking',9,'MEDIUM','What is a VLAN used for?','Logical Layer 2 network segmentation','Encrypting HTTP payloads','Assigning domain names','Compressing packets',0),
('networking',10,'HARD','What does source NAT commonly allow in a private network?','Many private hosts to share a public address','DNS to replace routing','Switches to assign passwords','TCP to become connectionless',0),

-- GraphQL
('graphql',6,'EASY','Which GraphQL operation changes server-side data?','query','mutation','fragment','scalar',1),
('graphql',7,'EASY','Why use GraphQL variables?','Pass dynamic values separately from query text','Define database indexes','Start subscriptions automatically','Replace the schema',0),
('graphql',8,'MEDIUM','What does an exclamation mark mean in a GraphQL type such as String!?','The value is non-null','The value is secret','The field is deprecated','The field is a list',0),
('graphql',9,'MEDIUM','What does schema introspection expose?','Type and field metadata','Production database passwords','Resolver source code','User sessions',0),
('graphql',10,'HARD','Why enforce query depth or complexity limits?','To reduce denial-of-service risk from expensive queries','To remove all nested fields','To make every query a mutation','To disable variables',0),

-- WebSocket
('websocket',6,'EASY','Which URI scheme represents WebSocket over TLS?','http','ws','wss','tcp',2),
('websocket',7,'EASY','What are ping and pong frames commonly used for?','Checking connection liveness','Sending database schemas','Authenticating passwords only','Compressing images',0),
('websocket',8,'MEDIUM','What preserves message boundaries in WebSocket?','Its framed message protocol','TCP packets individually','HTTP cookies','DNS records',0),
('websocket',9,'MEDIUM','When should a server authenticate a socket client?','During connection setup and before privileged messages','Only after disconnect','Never for WSS','Only when sending HTML',0),
('websocket',10,'HARD','What does a shared message broker enable across socket server instances?','Cross-instance fan-out and coordination','Browser DOM rendering','TLS certificate creation','Local-only state',0),

-- Agile and Scrum
('agile_scrum',6,'EASY','Who helps Scrum participants understand and apply Scrum?','Scrum Master','Product Owner only','External auditor','Database administrator',0),
('agile_scrum',7,'EASY','What is the Product Backlog?','An ordered list of work for improving the product','A fixed Sprint contract','A test report only','A source branch',0),
('agile_scrum',8,'MEDIUM','What is the purpose of a Sprint Retrospective?','Improve team quality and effectiveness','Accept or reject the Increment','Assign individual utilization targets','Estimate the annual budget',0),
('agile_scrum',9,'MEDIUM','What must a usable Increment satisfy?','The Definition of Done','Every future backlog item','A fixed velocity target','A manager sign-off only',0),
('agile_scrum',10,'HARD','Why should velocity not be used to compare teams?','Team estimates and contexts are not standardized productivity measures','Velocity has no relation to work','Only managers may see it','It is always constant',0),

-- Software architecture
('architecture',6,'EASY','What is a monolithic deployment?','Application capabilities packaged and deployed together','One database row','A browser-only component','A message topic',0),
('architecture',7,'EASY','What do ports represent in hexagonal architecture?','Boundaries through which the application interacts','Network TCP ports only','Database columns','UI pixels',0),
('architecture',8,'MEDIUM','What does dependency inversion encourage high-level policy to depend on?','Abstractions','Concrete infrastructure details','Global mutable state','Rendered HTML',0),
('architecture',9,'MEDIUM','Why is cache invalidation difficult?','Cached data must stay coherent with changing source data','Caches cannot store strings','Databases cannot be queried','Networks never fail',0),
('architecture',10,'HARD','When can CQRS add unjustified complexity?','When read and write needs are simple and similar','When models truly need independent scaling','When audit history is required','When teams understand eventual consistency',0),

-- Blockchain and Web3
('blockchain',6,'EASY','What is consensus used for in a blockchain network?','Agreeing on valid ledger state','Encrypting every public address','Creating CSS layouts','Storing private keys publicly',0),
('blockchain',7,'EASY','What does gas commonly measure on smart-contract platforms?','Computational resource use','Token ownership only','Network cable speed','Block color',0),
('blockchain',8,'MEDIUM','What is a nonce in proof-of-work mining used to vary?','The block header hash attempt','A wallet password','A smart contract ABI','A public key length',0),
('blockchain',9,'MEDIUM','Why is on-chain data difficult to change retroactively?','Later blocks and consensus secure its history','Every node is offline','Blocks contain no hashes','Private keys decrypt the chain',0),
('blockchain',10,'HARD','What risk arises if one actor controls a majority of proof-of-work hash power?','It may reorganize recent history or double-spend','It can derive all private keys','It can change cryptographic hash functions','It can erase every off-chain database',0);

-- Resolve the seed keys to the current quiz IDs before touching persisted data.
DROP TEMPORARY TABLE IF EXISTS _quiz_target_map;
CREATE TEMPORARY TABLE _quiz_target_map AS
SELECT
    target.seed_key,
    target.topic_code,
    course.id AS course_id,
    lesson.id AS lesson_id,
    quiz.id AS quiz_id
FROM _quiz_targets target
JOIN Courses course
  ON course.title = target.course_title
JOIN Lessons lesson
  ON lesson.course_id = course.id
 AND lesson.title = target.lesson_title
 AND LOWER(lesson.type) = 'quiz'
JOIN Quizzes quiz
  ON quiz.lesson_id = lesson.id;

ALTER TABLE _quiz_target_map
    ADD PRIMARY KEY (seed_key),
    ADD UNIQUE KEY uq_quiz_seed_quiz_id (quiz_id);

-- Fail before deletion if the repository data no longer matches this reviewed seed.
-- CHECK violations intentionally abort execution.
DROP TEMPORARY TABLE IF EXISTS _quiz_seed_guard;
CREATE TEMPORARY TABLE _quiz_seed_guard (
    check_name VARCHAR(80) NOT NULL PRIMARY KEY,
    ok TINYINT NOT NULL,
    CONSTRAINT chk_quiz_seed_guard CHECK (ok = 1)
) ENGINE = InnoDB;

INSERT INTO _quiz_seed_guard (check_name, ok)
SELECT 'all_51_quiz_targets_resolve_once',
       CASE
           WHEN COUNT(*) = 51
            AND COUNT(DISTINCT seed_key) = 51
            AND COUNT(DISTINCT quiz_id) = 51
           THEN 1 ELSE 0
       END
FROM _quiz_target_map;

INSERT INTO _quiz_seed_guard (check_name, ok)
SELECT 'all_51_quizzes_have_ten_questions',
       CASE
           WHEN COUNT(*) = 51
            AND MIN(question_count) = 10
            AND MAX(question_count) = 10
           THEN 1 ELSE 0
       END
FROM (
    SELECT target.seed_key, COUNT(question.seed_key) AS question_count
    FROM _quiz_targets target
    LEFT JOIN _quiz_questions question
      ON question.seed_key = target.seed_key
    GROUP BY target.seed_key
) counts_by_quiz;

INSERT INTO _quiz_seed_guard (check_name, ok)
SELECT 'all_question_rows_reference_a_target',
       CASE
           WHEN COUNT(*) = 510
            AND COUNT(DISTINCT question.seed_key) = 51
            AND SUM(target.seed_key IS NULL) = 0
           THEN 1 ELSE 0
       END
FROM _quiz_questions question
LEFT JOIN _quiz_targets target
  ON target.seed_key = question.seed_key;

START TRANSACTION;

-- Delete dependents first so foreign keys stay enabled throughout the script.
DELETE answer
FROM Quiz_Answers answer
JOIN Quiz_Attempts attempt
  ON attempt.id = answer.attempt_id
JOIN _quiz_target_map target
  ON target.quiz_id = attempt.quiz_id;

DELETE assigned
FROM Quiz_Attempt_Questions assigned
JOIN Quiz_Attempts attempt
  ON attempt.id = assigned.attempt_id
JOIN _quiz_target_map target
  ON target.quiz_id = attempt.quiz_id;

DELETE attempt
FROM Quiz_Attempts attempt
JOIN _quiz_target_map target
  ON target.quiz_id = attempt.quiz_id;

DELETE job
FROM Question_Generation_Jobs job
JOIN _quiz_target_map target
  ON target.quiz_id = job.quiz_id;

DELETE blueprint
FROM Quiz_Blueprint_Items blueprint
JOIN _quiz_target_map target
  ON target.quiz_id = blueprint.quiz_id;

DELETE question
FROM Questions question
JOIN _quiz_target_map target
  ON target.quiz_id = question.quiz_id;

-- Normalize quiz settings while preserving stable quiz IDs and lesson links.
UPDATE Quizzes quiz
JOIN _quiz_target_map target
  ON target.quiz_id = quiz.id
SET quiz.description = NULL,
    quiz.duration_minutes = 25,
    quiz.max_attempts = 3,
    quiz.passing_score = 70.00,
    quiz.status = 'published',
    quiz.updated_at = CURRENT_TIMESTAMP(6);

INSERT INTO Questions
    (quiz_id, question_text, options_json, correct_answer, question_type,
     points, display_order, topic_code, difficulty, review_status, version,
     active, generation_source, assignment_id)
SELECT
    target.quiz_id,
    source.question_text,
    JSON_ARRAY(source.option_a, source.option_b, source.option_c, source.option_d),
    CAST(source.correct_index AS CHAR),
    'single_choice',
    1.00,
    source.question_order,
    target.topic_code,
    source.difficulty,
    'APPROVED',
    1,
    b'1',
    'MANUAL',
    NULL
FROM _quiz_questions source
JOIN _quiz_target_map target
  ON target.seed_key = source.seed_key
ORDER BY target.quiz_id, source.question_order;

INSERT INTO Quiz_Blueprint_Items
    (quiz_id, topic_code, difficulty, question_count, display_order,
     created_at, updated_at)
SELECT
    target.quiz_id,
    target.topic_code,
    difficulty_bucket.difficulty,
    difficulty_bucket.question_count,
    difficulty_bucket.display_order,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM _quiz_target_map target
CROSS JOIN (
    SELECT 'EASY' AS difficulty, 4 AS question_count, 1 AS display_order
    UNION ALL
    SELECT 'MEDIUM', 4, 2
    UNION ALL
    SELECT 'HARD', 2, 3
) difficulty_bucket;

-- Post-write assertions: any failure rolls back when the client stops on error.
DELETE FROM _quiz_seed_guard;

INSERT INTO _quiz_seed_guard (check_name, ok)
SELECT 'inserted_510_active_approved_questions',
       CASE
           WHEN COUNT(*) = 510
            AND SUM(question.active = b'1') = 510
            AND SUM(question.review_status = 'APPROVED') = 510
           THEN 1 ELSE 0
       END
FROM Questions question
JOIN _quiz_target_map target
  ON target.quiz_id = question.quiz_id;

INSERT INTO _quiz_seed_guard (check_name, ok)
SELECT 'inserted_153_blueprint_buckets',
       CASE
           WHEN COUNT(*) = 153
            AND SUM(blueprint.question_count) = 510
           THEN 1 ELSE 0
       END
FROM Quiz_Blueprint_Items blueprint
JOIN _quiz_target_map target
  ON target.quiz_id = blueprint.quiz_id;

COMMIT;

SELECT
    COUNT(DISTINCT target.quiz_id) AS quizzes_replaced,
    COUNT(question.id) AS questions_inserted,
    SUM(question.difficulty = 'EASY') AS easy_questions,
    SUM(question.difficulty = 'MEDIUM') AS medium_questions,
    SUM(question.difficulty = 'HARD') AS hard_questions,
    @quiz_seed_started_at AS started_at,
    CURRENT_TIMESTAMP(6) AS completed_at
FROM _quiz_target_map target
JOIN Questions question
  ON question.quiz_id = target.quiz_id;

DROP TEMPORARY TABLE IF EXISTS _quiz_seed_guard;
DROP TEMPORARY TABLE IF EXISTS _quiz_target_map;
DROP TEMPORARY TABLE IF EXISTS _quiz_questions;
DROP TEMPORARY TABLE IF EXISTS _quiz_targets;
