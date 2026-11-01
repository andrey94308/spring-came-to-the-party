import fs from 'node:fs/promises';
import path from 'node:path';
import { Presentation, PresentationFile } from '@oai/artifact-tool';

const root = process.cwd();
const skill = '/Users/andro/.codex/plugins/cache/openai-primary-runtime/presentations/26.1029.11837/skills/presentations';
const deck = Presentation.create({slideSize:{width:1280,height:720}});
const slides = [
  {title:'MATO archive',sub:'From an empty password to documents across departments',body:['A live Postman chain','Spring LDAP 3.3.7 · Spring AI MariaDB Store 1.0.3']},
  {title:'The archive boundary',sub:'Only the gateway is reachable from the host',body:['Postman  →  edge gateway :8080  →  archive service :8081','Archive service  →  OpenLDAP identities','Archive service  →  MariaDB roles, notes and documents']},
  {title:'The baseline',sub:'Access rules before the exploit',body:['Anonymous /users and /notes: 401','Admin /notes and document search: 403','Regular user /users: 403','Bob requests logistics-corps documents: []']},
  {title:'An empty password opens the roster',sub:'CVE-2026-41720 depends on the LDAP server setting',body:['Basic username: mr.president','Basic password: empty','Spring LDAP binds against OpenLDAP with bind_anon_dn enabled','The database profile maps the result to ROLE_ADMIN']},
  {title:'The roster exposes BCrypt hashes',sub:'An intentionally unsafe admin DTO makes offline recovery possible',body:['GET /users returns bob.passwordHash','Compare the hash with solve/wordlist.txt locally','Bob is the only regular user whose password is in that list','Set the recovered word in a local Postman variable']},
  {title:'Notes belong to a department',sub:'Bob and Maija share intel-analysis notes',body:['GET /notes has no scope selector','The archive reads department from the authenticated database user','Bob and Maija receive the same records','Renaud receives separate logistics-corps notes']},
  {title:'The ordinary search stays scoped',sub:'Bob cannot request another department by name',body:["WHERE JSON_VALUE(metadata, '$.department') = ?",'AND <converter output for requested department>','The first value comes from Bob’s database profile','The plain logistics-corps filter returns []']},
  {title:'A quote changes the SQL predicate',sub:'CVE-2026-22730 in Spring AI 1.0.3',body:["Requested department: intel-analysis' OR '1'='1",'The converter emits the quote without escaping it','SQL evaluates the scoped AND before the injected OR','The response includes unassigned mato-special-ops']},
  {title:'The same input stays data when bound',sub:'The application-level safe query preserves isolation',body:['/documents/search-safe binds both values','The injected string returns []','This endpoint demonstrates parameter binding','It does not represent a patched Spring AI build']},
  {title:'Live sequence',sub:'Twelve Postman requests, one local hash comparison',body:['1–4  Baseline and empty-password admin access','5–8  Offline recovery, shared notes and role checks','9–12  Normal, foreign, injected and safe document searches']}
];
function box(slide,text,x,y,w,h,size,color,bold=false){
 const shape=slide.shapes.add({geometry:'textbox',position:{left:x,top:y,width:w,height:h},fill:'none',line:{style:'solid',fill:'none',width:0}});
 shape.text=text; shape.text.style={fontFamily:'Arial',fontSize:size,color,bold}; return shape;
}
for (let i=0;i<slides.length;i++) {
 const d=slides[i], slide=deck.slides.add();
 slide.background.fill='#101827';
 box(slide,String(i+1).padStart(2,'0'),80,48,100,42,18,'#5EC6BA',true);
 box(slide,d.title,80,112,1110,88,i===0?54:42,'#FFFFFF',true);
 box(slide,d.sub,80,214,1110,72,24,'#5EC6BA');
 for(let j=0;j<d.body.length;j++) box(slide,d.body[j],80,320+j*72,1110,64,21,'#D7E0EC');
}
const out=path.join(root,'.codex-slide-build','candidate.pptx');
await (await PresentationFile.exportPptx(deck)).save(out);
const {finalizePresentation}=await import(path.join(skill,'container_tools/artifact_tool_utils.mjs'));
const final=path.join(root,'presentation','MATO-postman-chain.pptx');
const result=await finalizePresentation({workspaceDir:root,candidatePath:out,finalPath:final,
 pythonExecutable:'/Users/andro/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3',
 integrityValidatorPath:path.join(skill,'container_tools/inspect_presentation_package_integrity.py'),
 layoutValidatorPath:path.join(skill,'container_tools/inspect_presentation_layout_geometry.py'),
 layoutArgs:['--expected-slide-size-emu','12192000,6858000','--validate-heading-fit'],
 requiredNativeTableOwnerSlides:[],fontPolicy:{basis:'design',families:['Aptos']},
 verifyArtifactToolImport:true,receiptPath:path.join(root,'.codex-slide-build','validation.json')});
console.log(JSON.stringify(result));
