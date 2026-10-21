export const sampleJobs = [
  {id:1,company:'네오테크',logo:'N',color:'#6556df',title:'2026 상반기 신입 개발자 공개채용',category:'개발',type:'신입',location:'서울',deadline:'D-3',tags:['백엔드','프론트엔드']},
  {id:2,company:'그린모빌리티',logo:'G',color:'#16ad68',title:'서비스 기획 및 데이터 분석 인턴',category:'마케팅',type:'인턴',location:'경기',deadline:'D-5',tags:['서비스기획','데이터']},
  {id:3,company:'라이트스튜디오',logo:'L',color:'#ff895c',title:'Product Designer 신입 채용',category:'디자인',type:'신입',location:'서울',deadline:'D-7',tags:['UX/UI','프로덕트']},
  {id:4,company:'한빛금융',logo:'H',color:'#2672e8',title:'디지털 금융 서비스 개발자 모집',category:'개발',type:'신입',location:'서울',deadline:'D-9',tags:['핀테크','Java']},
  {id:5,company:'오늘마켓',logo:'O',color:'#f2a800',title:'브랜드 콘텐츠 마케터 채용연계형 인턴',category:'마케팅',type:'인턴',location:'서울',deadline:'D-12',tags:['콘텐츠','브랜드']},
  {id:6,company:'클라우드나인',logo:'C',color:'#2e9ecb',title:'인사 운영 및 조직문화 담당자',category:'경영지원',type:'신입',location:'경기',deadline:'D-14',tags:['HR','조직문화']},
  {id:7,company:'모노웍스',logo:'M',color:'#e05e7e',title:'BX 디자이너 경력직 채용',category:'디자인',type:'경력',location:'서울',deadline:'D-18',tags:['브랜딩','그래픽']},
  {id:8,company:'데이터포레스트',logo:'D',color:'#1f8768',title:'AI Research Engineer 인턴십',category:'개발',type:'인턴',location:'대전',deadline:'D-20',tags:['AI','Python']},
  {id:9,company:'피크커머스',logo:'P',color:'#8c62d8',title:'재무회계 신입사원 수시채용',category:'경영지원',type:'신입',location:'서울',deadline:'D-21',tags:['재무','회계']}
]

export const filterSampleJobs = (jobs, query = '', category = '전체') => jobs.filter(job =>
  (category === '전체' || job.category === category) &&
  (!query || [job.company, job.title, job.category, job.type, ...job.tags].join(' ').toLowerCase().includes(query.trim().toLowerCase()))
)
