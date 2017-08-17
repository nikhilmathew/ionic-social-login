import { LoginsuService } from './../../Services/loginsu.service';
import { Angular2SocialLoginService } from './../../Services/login.service';
import { LoginComponent } from './../login.component';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-socialsignedin',
  templateUrl: './socialsignedin.component.html',
  styleUrls: ['./socialsignedin.component.scss']
})
export class SocialsignedinComponent implements OnInit {
  userData = {
    email: '', image: '', name: '',provider:'',uid:''
  }
  constructor(private login: Angular2SocialLoginService,private sulogin:LoginsuService) { }

  ngOnInit() {
    this.userData= this.login.loginData
    console.log(this.userData)
  }
  newUser(){
    console.log("clicked on new user")
    this.sulogin.loginSU({name:this.userData.name,email:this.userData.email,mode:this.userData.provider,mode_id:this.userData.uid,udid:navigator.userAgent,apk_version:'0'}).subscribe((data)=>{
    console.log(data.user)
    this.sulogin.loggedin = data.user
    // this.router.navigate(['/home'],{ skipLocationChange: true })
    })
  }
  oldUser(){
    console.log("clicked on old user")
    
  }


}
