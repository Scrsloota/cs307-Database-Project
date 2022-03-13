--
-- PostgreSQL database dump
--

-- Dumped from database version 12.6
-- Dumped by pg_dump version 12.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: course; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.course (
    courseid character varying NOT NULL,
    coursename character varying NOT NULL,
    credit integer NOT NULL,
    classhour integer NOT NULL,
    grading character varying NOT NULL
);


ALTER TABLE public.course OWNER TO checker;

--
-- Name: coursepre; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.coursepre (
    courseid character varying NOT NULL,
    preid character varying NOT NULL,
    groupid integer NOT NULL
);


ALTER TABLE public.coursepre OWNER TO checker;

--
-- Name: coursesection; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.coursesection (
    sectionid integer NOT NULL,
    sectionname character varying NOT NULL,
    totalcapacity integer NOT NULL,
    leftcapacity integer NOT NULL,
    courseid character varying NOT NULL,
    semesterid integer NOT NULL
);


ALTER TABLE public.coursesection OWNER TO checker;

--
-- Name: coursesection_sectionid_seq; Type: SEQUENCE; Schema: public; Owner: checker
--

CREATE SEQUENCE public.coursesection_sectionid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.coursesection_sectionid_seq OWNER TO checker;

--
-- Name: coursesection_sectionid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: checker
--

ALTER SEQUENCE public.coursesection_sectionid_seq OWNED BY public.coursesection.sectionid;


--
-- Name: coursesectionclass; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.coursesectionclass (
    coursesectionclassid integer NOT NULL,
    instructorid integer NOT NULL,
    dayofweek character varying NOT NULL,
    weeklist character varying[] NOT NULL,
    classbegin integer NOT NULL,
    classend integer NOT NULL,
    location character varying NOT NULL,
    sectionid integer NOT NULL
);


ALTER TABLE public.coursesectionclass OWNER TO checker;

--
-- Name: coursesectionclass_coursesectionclassid_seq; Type: SEQUENCE; Schema: public; Owner: checker
--

CREATE SEQUENCE public.coursesectionclass_coursesectionclassid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.coursesectionclass_coursesectionclassid_seq OWNER TO checker;

--
-- Name: coursesectionclass_coursesectionclassid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: checker
--

ALTER SEQUENCE public.coursesectionclass_coursesectionclassid_seq OWNED BY public.coursesectionclass.coursesectionclassid;


--
-- Name: department; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.department (
    departmentid integer NOT NULL,
    name character varying NOT NULL
);


ALTER TABLE public.department OWNER TO checker;

--
-- Name: department_departmentid_seq; Type: SEQUENCE; Schema: public; Owner: checker
--

CREATE SEQUENCE public.department_departmentid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.department_departmentid_seq OWNER TO checker;

--
-- Name: department_departmentid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: checker
--

ALTER SEQUENCE public.department_departmentid_seq OWNED BY public.department.departmentid;


--
-- Name: enrollcoursegrade; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.enrollcoursegrade (
    studentid integer NOT NULL,
    sectionid integer NOT NULL,
    grade character varying
);


ALTER TABLE public.enrollcoursegrade OWNER TO checker;

--
-- Name: enrollcoursesection; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.enrollcoursesection (
    studentid integer NOT NULL,
    sectionid integer NOT NULL
);


ALTER TABLE public.enrollcoursesection OWNER TO checker;

--
-- Name: instructor; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.instructor (
    userid integer NOT NULL,
    firstname character varying NOT NULL,
    lastname character varying NOT NULL
);


ALTER TABLE public.instructor OWNER TO checker;

--
-- Name: major; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.major (
    majorid integer NOT NULL,
    name character varying NOT NULL,
    departmentid integer NOT NULL
);


ALTER TABLE public.major OWNER TO checker;

--
-- Name: major_majorid_seq; Type: SEQUENCE; Schema: public; Owner: checker
--

CREATE SEQUENCE public.major_majorid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.major_majorid_seq OWNER TO checker;

--
-- Name: major_majorid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: checker
--

ALTER SEQUENCE public.major_majorid_seq OWNED BY public.major.majorid;


--
-- Name: majorcourse; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.majorcourse (
    majorid integer NOT NULL,
    courseid character varying NOT NULL,
    property character varying NOT NULL
);


ALTER TABLE public.majorcourse OWNER TO checker;

--
-- Name: semester; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.semester (
    semesterid integer NOT NULL,
    name character varying NOT NULL,
    begindate date NOT NULL,
    enddate date NOT NULL
);


ALTER TABLE public.semester OWNER TO checker;

--
-- Name: semester_semesterid_seq; Type: SEQUENCE; Schema: public; Owner: checker
--

CREATE SEQUENCE public.semester_semesterid_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.semester_semesterid_seq OWNER TO checker;

--
-- Name: semester_semesterid_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: checker
--

ALTER SEQUENCE public.semester_semesterid_seq OWNED BY public.semester.semesterid;


--
-- Name: student; Type: TABLE; Schema: public; Owner: checker
--

CREATE TABLE public.student (
    userid integer NOT NULL,
    firstname character varying NOT NULL,
    lastname character varying NOT NULL,
    enrolleddate date NOT NULL,
    majorid integer
);


ALTER TABLE public.student OWNER TO checker;

--
-- Name: coursesection sectionid; Type: DEFAULT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.coursesection ALTER COLUMN sectionid SET DEFAULT nextval('public.coursesection_sectionid_seq'::regclass);


--
-- Name: coursesectionclass coursesectionclassid; Type: DEFAULT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.coursesectionclass ALTER COLUMN coursesectionclassid SET DEFAULT nextval('public.coursesectionclass_coursesectionclassid_seq'::regclass);


--
-- Name: department departmentid; Type: DEFAULT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.department ALTER COLUMN departmentid SET DEFAULT nextval('public.department_departmentid_seq'::regclass);


--
-- Name: major majorid; Type: DEFAULT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.major ALTER COLUMN majorid SET DEFAULT nextval('public.major_majorid_seq'::regclass);


--
-- Name: semester semesterid; Type: DEFAULT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.semester ALTER COLUMN semesterid SET DEFAULT nextval('public.semester_semesterid_seq'::regclass);


--
-- Name: course course_pkey; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.course
    ADD CONSTRAINT course_pkey PRIMARY KEY (courseid);


--
-- Name: coursesection coursesection_pkey; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.coursesection
    ADD CONSTRAINT coursesection_pkey PRIMARY KEY (sectionid);


--
-- Name: coursesectionclass coursesectionclass_pkey; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.coursesectionclass
    ADD CONSTRAINT coursesectionclass_pkey PRIMARY KEY (coursesectionclassid);


--
-- Name: department department_pkey; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT department_pkey PRIMARY KEY (departmentid);


--
-- Name: enrollcoursegrade enrollcoursegrade_pkey; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.enrollcoursegrade
    ADD CONSTRAINT enrollcoursegrade_pkey PRIMARY KEY (studentid, sectionid);


--
-- Name: enrollcoursesection enrollcoursesection_pkey; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.enrollcoursesection
    ADD CONSTRAINT enrollcoursesection_pkey PRIMARY KEY (studentid, sectionid);


--
-- Name: instructor instructor_pkey; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.instructor
    ADD CONSTRAINT instructor_pkey PRIMARY KEY (userid);


--
-- Name: major major_pkey; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.major
    ADD CONSTRAINT major_pkey PRIMARY KEY (majorid);


--
-- Name: majorcourse majorcourse_pkey; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.majorcourse
    ADD CONSTRAINT majorcourse_pkey PRIMARY KEY (courseid, majorid);


--
-- Name: semester semester_pkey; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.semester
    ADD CONSTRAINT semester_pkey PRIMARY KEY (semesterid);


--
-- Name: student student_pkey; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.student
    ADD CONSTRAINT student_pkey PRIMARY KEY (userid);


--
-- Name: coursepre uq; Type: CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.coursepre
    ADD CONSTRAINT uq UNIQUE (courseid, preid, groupid);


--
-- Name: coureindex; Type: INDEX; Schema: public; Owner: checker
--

CREATE INDEX coureindex ON public.coursesection USING btree (sectionid);


--
-- Name: coursesection course_id; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.coursesection
    ADD CONSTRAINT course_id FOREIGN KEY (courseid) REFERENCES public.course(courseid);


--
-- Name: majorcourse course_id; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.majorcourse
    ADD CONSTRAINT course_id FOREIGN KEY (courseid) REFERENCES public.course(courseid);


--
-- Name: coursepre coursepreid; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.coursepre
    ADD CONSTRAINT coursepreid FOREIGN KEY (courseid) REFERENCES public.course(courseid);


--
-- Name: major departmentid; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.major
    ADD CONSTRAINT departmentid FOREIGN KEY (departmentid) REFERENCES public.department(departmentid) ON DELETE CASCADE;


--
-- Name: coursesectionclass instructor_id; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.coursesectionclass
    ADD CONSTRAINT instructor_id FOREIGN KEY (instructorid) REFERENCES public.instructor(userid);


--
-- Name: majorcourse major_id; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.majorcourse
    ADD CONSTRAINT major_id FOREIGN KEY (majorid) REFERENCES public.major(majorid);


--
-- Name: student majorid; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.student
    ADD CONSTRAINT majorid FOREIGN KEY (majorid) REFERENCES public.major(majorid) ON DELETE CASCADE;


--
-- Name: coursepre preid; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.coursepre
    ADD CONSTRAINT preid FOREIGN KEY (preid) REFERENCES public.course(courseid);


--
-- Name: coursesectionclass section_id; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.coursesectionclass
    ADD CONSTRAINT section_id FOREIGN KEY (sectionid) REFERENCES public.coursesection(sectionid);


--
-- Name: enrollcoursesection section_id; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.enrollcoursesection
    ADD CONSTRAINT section_id FOREIGN KEY (sectionid) REFERENCES public.coursesection(sectionid) ON DELETE CASCADE;


--
-- Name: coursesection semester_id; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.coursesection
    ADD CONSTRAINT semester_id FOREIGN KEY (semesterid) REFERENCES public.semester(semesterid);


--
-- Name: enrollcoursegrade semester_id; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.enrollcoursegrade
    ADD CONSTRAINT semester_id FOREIGN KEY (sectionid) REFERENCES public.coursesection(sectionid) ON DELETE CASCADE;


--
-- Name: enrollcoursesection student_id; Type: FK CONSTRAINT; Schema: public; Owner: checker
--

ALTER TABLE ONLY public.enrollcoursesection
    ADD CONSTRAINT student_id FOREIGN KEY (studentid) REFERENCES public.student(userid);


--
-- PostgreSQL database dump complete
--

